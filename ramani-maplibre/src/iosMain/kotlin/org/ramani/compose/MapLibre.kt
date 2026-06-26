/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import MapLibre.MLNMapView
import MapLibre.MLNMapViewDelegateProtocol
import MapLibre.MLNStyle
import MapLibre.MLNUserTrackingModeFollow
import MapLibre.MLNUserTrackingModeFollowWithCourse
import MapLibre.MLNUserTrackingModeFollowWithHeading
import MapLibre.MLNUserTrackingModeNone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.awaitCancellation
import platform.CoreGraphics.CGRectMake
import platform.CoreLocation.CLLocationManager
import platform.Foundation.NSURL
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapLibre(
    modifier: Modifier,
    style: MapStyle,
    cameraPositionState: CameraPositionState,
    uiSettings: UiSettings,
    properties: MapProperties,
    locationRequestProperties: LocationRequestProperties?,
    locationStyling: LocationStyling,
    userLocation: MutableState<UserLocation>?,
    renderMode: RenderMode,
    cameraMode: MutableState<CameraMode>,
    onMapClick: (LatLng) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    content: (@Composable () -> Unit)?,
) {
    val currentStyle by rememberUpdatedState(style)
    val currentContent by rememberUpdatedState(content)
    val currentLocationRequestProperties by rememberUpdatedState(locationRequestProperties)
    val currentCameraMode by rememberUpdatedState(cameraMode.value)
    val parentComposition = rememberCompositionContext()

    val styleLoaded = remember { mutableStateOf(false) }
    val mapViewState = remember { mutableStateOf<MLNMapView?>(null) }

    val delegate = remember {
        object : NSObject(), MLNMapViewDelegateProtocol {
            override fun mapView(mapView: MLNMapView, didFinishLoadingStyle: MLNStyle) {
                styleLoaded.value = true
            }

            override fun mapViewRegionIsChanging(mapView: MLNMapView) {
                cameraPositionState.updatePositionFromMap(
                    target = mapView.centerCoordinate.toCommon(),
                    zoom = mapView.zoomLevel,
                    bearing = mapView.direction,
                )
            }

            override fun mapView(mapView: MLNMapView, regionDidChangeAnimated: Boolean) {
                cameraPositionState.updatePositionFromMap(
                    target = mapView.centerCoordinate.toCommon(),
                    zoom = mapView.zoomLevel,
                    bearing = mapView.direction,
                )
            }

            override fun mapView(mapView: MLNMapView, didUpdateUserLocation: MapLibre.MLNUserLocation?) {
                didUpdateUserLocation?.location?.let { loc ->
                    loc.coordinate.useContents {
                        userLocation?.value = UserLocation(
                            latitude = latitude,
                            longitude = longitude,
                            altitude = loc.altitude,
                            bearing = loc.course.toFloat(),
                        )
                    }
                }
            }
        }
    }

    UIKitView(
        factory = {
            MapLibreInitializer.initialize()
            val styleUrl = when (val s = currentStyle) {
                is MapStyle.Uri -> NSURL(string = s.uri)
                is MapStyle.Json -> NSURL(string = s.json)
            }
            val mapView = MLNMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), styleURL = styleUrl)
            mapView.delegate = delegate
            mapViewState.value = mapView
            mapView
        },
        modifier = modifier,
        update = { mapView ->
            when (val s = currentStyle) {
                is MapStyle.Uri -> {
                    val newUrl = NSURL(string = s.uri)
                    if (mapView.styleURL.absoluteString != newUrl.absoluteString) {
                        styleLoaded.value = false
                        mapView.styleURL = newUrl
                    }
                }
                is MapStyle.Json -> {
                    val newUrl = NSURL(string = s.json)
                    if (mapView.styleURL.absoluteString != newUrl.absoluteString) {
                        styleLoaded.value = false
                        mapView.styleURL = newUrl
                    }
                }
            }

            // Location
            if (currentLocationRequestProperties != null) {
                mapView.showsUserLocation = true
                mapView.userTrackingMode = when (currentCameraMode) {
                    CameraMode.NONE -> MLNUserTrackingModeNone
                    CameraMode.TRACKING -> MLNUserTrackingModeFollow
                    CameraMode.TRACKING_GPS -> MLNUserTrackingModeFollowWithCourse
                    CameraMode.TRACKING_COMPASS -> MLNUserTrackingModeFollowWithHeading
                    CameraMode.TRACKING_GPS_NORTH -> MLNUserTrackingModeFollow
                }
            } else {
                mapView.showsUserLocation = false
            }
        },
    )

    val mapView = mapViewState.value

    LaunchedEffect(mapView, styleLoaded.value) {
        if (mapView == null || !styleLoaded.value) return@LaunchedEffect

        // Apply initial camera position
        val pos = cameraPositionState.position
        pos.target?.let {
            mapView.setCenterCoordinate(it.toCLLocationCoordinate2D(), animated = false)
        }
        pos.zoom?.let {
            mapView.setZoomLevel(it, animated = false)
        }
        pos.bearing?.let {
            mapView.setDirection(it, animated = false)
        }

        val mapApplier = MapApplier(mapView)
        mapApplier.dragHandler = AnnotationDragHandler(mapView, mapApplier)
        val composition = newComposition(parentComposition, mapApplier) {
            @Suppress("UNCHECKED_CAST")
            val applier = currentComposer.applier as MapApplier
            val iosProjection = remember(applier.mapView) { MapProjectionIos(applier.mapView) }
            CompositionLocalProvider(
                LocalMapApplier provides applier,
                LocalMapProjection provides iosProjection,
            ) {
                DisposableEffect(iosProjection) {
                    cameraPositionState.setProjection(iosProjection)
                    onDispose { cameraPositionState.setProjection(null) }
                }
                ComposeNode<MapPropertiesNode, MapApplier>(factory = {
                    MapPropertiesNode(
                        mapView = mapView,
                        cameraPositionState = cameraPositionState,
                    )
                }, update = {
                    update(cameraPositionState.moveGeneration) {
                        val cameraPosition = cameraPositionState.position
                        val animated = cameraPosition.motionType != CameraMotionType.INSTANT
                        cameraPosition.target?.let {
                            mapView.setCenterCoordinate(it.toCLLocationCoordinate2D(), animated = animated)
                        }
                        cameraPosition.zoom?.let {
                            mapView.setZoomLevel(it, animated = animated)
                        }
                        cameraPosition.bearing?.let {
                            mapView.setDirection(it, animated = animated)
                        }
                    }
                })
                currentContent?.invoke()
            }
        }

        try {
            awaitCancellation()
        } finally {
            // A new composition is built every time the style finishes loading
            // (styleLoaded flips). Dispose the previous one when the effect
            // relaunches or leaves; otherwise each style swap leaks a live
            // Composition and MapApplier bound to the same MLNMapView, and rapid
            // swaps race them into a crash.
            composition.dispose()
        }
    }
}
