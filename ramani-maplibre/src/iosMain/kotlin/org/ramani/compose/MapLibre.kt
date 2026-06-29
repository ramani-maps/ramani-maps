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
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import kotlinx.coroutines.awaitCancellation
import platform.CoreGraphics.CGRectMake
import platform.CoreLocation.CLLocationManager
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UITapGestureRecognizer
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
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnMapLongClick by rememberUpdatedState(onMapLongClick)
    val parentComposition = rememberCompositionContext()

    val styleLoaded = remember { mutableStateOf(false) }
    val mapViewState = remember { mutableStateOf<MLNMapView?>(null) }

    // The last applied style, tracked so the map reloads only on a real change (by value).
    val appliedStyle = remember { mutableStateOf<MapStyle?>(null) }

    // Gates the delegate's camera write-back until the initial camera has been applied, so the
    // world-view position during the first layout can't overwrite cameraPositionState.
    val cameraInitialized = remember { mutableStateOf(false) }

    val gestureHandler = remember {
        object : NSObject() {
            @ObjCAction
            fun onTap(recognizer: UITapGestureRecognizer) {
                val map = recognizer.view as? MLNMapView ?: return
                val coord = map.convertPoint(recognizer.locationInView(map), toCoordinateFromView = map)
                currentOnMapClick(coord.toCommon())
            }

            @ObjCAction
            fun onLongPress(recognizer: UILongPressGestureRecognizer) {
                if (recognizer.state != UIGestureRecognizerStateBegan) return
                val map = recognizer.view as? MLNMapView ?: return
                val coord = map.convertPoint(recognizer.locationInView(map), toCoordinateFromView = map)
                currentOnMapLongClick(coord.toCommon())
            }
        }
    }

    val delegate = remember {
        object : NSObject(), MLNMapViewDelegateProtocol {
            override fun mapView(mapView: MLNMapView, didFinishLoadingStyle: MLNStyle) {
                styleLoaded.value = true
            }

            override fun mapViewRegionIsChanging(mapView: MLNMapView) {
                if (!cameraInitialized.value) return
                cameraPositionState.updatePositionFromMap(
                    target = mapView.centerCoordinate.toCommon(),
                    zoom = mapView.zoomLevel,
                    bearing = mapView.direction,
                )
            }

            override fun mapView(mapView: MLNMapView, regionDidChangeAnimated: Boolean) {
                if (!cameraInitialized.value) return
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
            val frame = CGRectMake(0.0, 0.0, 0.0, 0.0)
            val mapView = when (val s = currentStyle) {
                is MapStyle.Uri -> MLNMapView(frame = frame, styleURL = NSURL(string = s.uri))
                is MapStyle.Json -> MLNMapView(frame = frame, styleJSON = s.json)
            }
            appliedStyle.value = currentStyle

            // Start the map at the requested position so the first frame isn't the world view.
            // The camera write-back stays gated (see cameraInitialized) until the post-style-load
            // effect re-applies and unlocks it.
            val initial = cameraPositionState.position
            initial.target?.let {
                mapView.setCenterCoordinate(it.toCLLocationCoordinate2D(), animated = false)
            }
            initial.zoom?.let { mapView.setZoomLevel(it, animated = false) }
            initial.bearing?.let { mapView.setDirection(it, animated = false) }

            mapView.delegate = delegate
            // A JSON style is parsed synchronously during init, so didFinishLoadingStyle fires
            // before the delegate above is attached and is missed. Pick it up here; otherwise
            // styleLoaded never flips for MapStyle.Json and the post-load composition never runs.
            if (mapView.style != null) styleLoaded.value = true
            mapView.addGestureRecognizer(
                UITapGestureRecognizer(gestureHandler, NSSelectorFromString("onTap:"))
            )
            mapView.addGestureRecognizer(
                UILongPressGestureRecognizer(gestureHandler, NSSelectorFromString("onLongPress:"))
            )
            mapViewState.value = mapView
            mapView
        },
        modifier = modifier,
        update = { mapView ->
            if (currentStyle != appliedStyle.value) {
                appliedStyle.value = currentStyle
                styleLoaded.value = false
                when (val s = currentStyle) {
                    is MapStyle.Uri -> mapView.styleURL = NSURL(string = s.uri)
                    is MapStyle.Json -> mapView.styleJSON = s.json
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

        // Apply the camera once the style has loaded and the view has a real size, then unlock the
        // delegate's write-back. Uses the live position so a style swap (e.g. offline/online
        // toggle) preserves where the user currently is rather than snapping back to the start.
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
        cameraInitialized.value = true

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
