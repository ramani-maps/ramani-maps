/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2023 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Camera
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTargetMarker
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.engine.LocationEngine
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback
import com.mapbox.mapboxsdk.location.engine.LocationEngineDefault
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMoveListener
import com.mapbox.mapboxsdk.maps.MapboxMap.OnRotateListener
import com.mapbox.mapboxsdk.maps.MapboxMap.OnScaleListener
import com.mapbox.mapboxsdk.maps.MapboxMap.OnShoveListener
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.sources.Source
import com.mapbox.mapboxsdk.utils.BitmapUtils
import org.ramani.compose.camera.CameraPitch

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "Maplibre Composable")
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class MapLibreComposable

/**
 * A composable representing a MapLibre map.
 *
 * @param modifier The modifier applied to the map.
 * @param styleUrl The style url to access the tile provider. Defaults to a demo tile provider.
 * @param cameraPosition The position of the map camera.
 * @param uiSettings Settings related to the map UI.
 * @param properties Properties being applied to the map.
 * @param locationRequestProperties Properties related to the location marker. If null (which is
 *        the default), then the location will not be enabled on the map. Enabling the location
 *        requires setting this field and getting the location permission in your app.
 * @param locationStyling Styling related to the location marker (color, pulse, etc).
 * @param userLocation If set and if the location is enabled (by setting [locationRequestProperties],
 *        it will be updated to contain the latest user location as known by the map.
 * @param sources External (user-defined) sources for the map.
 * @param layers External (user-defined) layers for the map.
 * @param images Images to be added to the map and used by external layers (pairs of <id, drawable code>).
 * @param content The content of the map.
 */
@Composable
fun MapLibre(
    modifier: Modifier,
    styleUrl: String = "https://demotiles.maplibre.org/style.json",
    cameraPosition: CameraPosition = rememberSaveable { CameraPosition() },
    uiSettings: UiSettings = UiSettings(),
    properties: MapProperties = MapProperties(),
    locationEngine: LocationEngine? = null,
    locationRequestProperties: LocationRequestProperties? = null,
    locationStyling: LocationStyling = LocationStyling(),
    userLocation: MutableState<Location>? = null,
    sources: List<Source>? = null,
    layers: List<Layer>? = null,
    images: List<Pair<String, Int>>? = null,
    cameraPositionCallback: CameraPositionCallback? = null,
    content: (@Composable @MapLibreComposable () -> Unit)? = null,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    val context = LocalContext.current
    val map = rememberMapViewWithLifecycle()
    val currentCameraPosition by rememberUpdatedState(cameraPosition)
    val currentUiSettings by rememberUpdatedState(uiSettings)
    val currentMapProperties by rememberUpdatedState(properties)
    val currentLocationEngine by rememberUpdatedState(locationEngine)
    val currentLocationRequestProperties by rememberUpdatedState(locationRequestProperties)
    val currentLocationStyling by rememberUpdatedState(locationStyling)
    val currentSources by rememberUpdatedState(sources)
    val currentLayers by rememberUpdatedState(layers)
    val currentImages by rememberUpdatedState(images)
    val currentContent by rememberUpdatedState(content)
    val parentComposition = rememberCompositionContext()

    AndroidView(modifier = modifier, factory = { map })
    LaunchedEffect(
        currentUiSettings,
        currentMapProperties,
        currentLocationRequestProperties,
        currentLocationStyling
    ) {
        disposingComposition {
            val maplibreMap = map.awaitMap()
            val style = maplibreMap.awaitStyle(styleUrl)

            maplibreMap.applyUiSettings(currentUiSettings)
            maplibreMap.applyProperties(currentMapProperties)
            maplibreMap.setupLocation(
                context,
                style,
                currentLocationEngine,
                currentLocationRequestProperties,
                currentLocationStyling,
                userLocation,
            )
            maplibreMap.addImages(context, currentImages)
            maplibreMap.addSources(currentSources)
            maplibreMap.addLayers(currentLayers)

            map.newComposition(parentComposition, style) {
                CompositionLocalProvider {
                    MapUpdater(
                        cameraPosition = currentCameraPosition,
                        cameraPositionCallback = cameraPositionCallback
                    )
                    currentContent?.invoke()
                }
            }
        }
    }
}

private fun MapboxMap.applyUiSettings(uiSettings: UiSettings) {
    this.uiSettings.setCompassMargins(
        uiSettings.compassMargins.left,
        uiSettings.compassMargins.top,
        uiSettings.compassMargins.right,
        uiSettings.compassMargins.bottom
    )
}

private fun MapboxMap.applyProperties(properties: MapProperties) {
    properties.maxZoom?.let { this.setMaxZoomPreference(it) }
}

private fun MapboxMap.setupLocation(
    context: Context,
    style: Style,
    locationEngine: LocationEngine?,
    locationRequestProperties: LocationRequestProperties?,
    locationStyling: LocationStyling,
    userLocation: MutableState<Location>?,
) {
    if (locationEngine == null || locationRequestProperties == null) return

    val locationEngineRequest = locationRequestProperties.toMapLibre()

    val locationActivationOptions = LocationComponentActivationOptions
        .builder(context, style)
        .locationEngine(locationEngine)
        .locationComponentOptions(locationStyling.toMapLibre(context))
        .locationEngineRequest(locationEngineRequest)
        .build()

    this.locationComponent.activateLocationComponent(locationActivationOptions)

    if (isFineLocationGranted(context) || isCoarseLocationGranted(context)) {
        @SuppressLint("MissingPermission")
        this.locationComponent.isLocationComponentEnabled = true
        userLocation?.let { trackLocation(context, locationEngineRequest, userLocation) }
    }
}

private fun isFineLocationGranted(context: Context): Boolean {
    return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private fun isCoarseLocationGranted(context: Context): Boolean {
    return context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private fun trackLocation(
    context: Context,
    locationEngineRequest: LocationEngineRequest,
    userLocation: MutableState<Location>
) {
    assert(isFineLocationGranted(context) || isCoarseLocationGranted(context))

    val locationEngine = LocationEngineDefault.getDefaultLocationEngine(context)
    locationEngine.requestLocationUpdates(
        locationEngineRequest,
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                Log.d("MapLibre", "Location update: ${result?.lastLocation}")
                result?.lastLocation?.let {
                    userLocation.value = it
                }
            }

            override fun onFailure(exception: Exception) {
                throw exception
            }
        },
        null
    )
}

private fun LocationStyling.toMapLibre(context: Context): LocationComponentOptions {
    val builder = LocationComponentOptions.builder(context)
    this.accuracyAlpha?.let { builder.accuracyAlpha(it) }
    this.accuracyColor?.let { builder.accuracyColor(it) }
    this.enablePulse?.let { builder.pulseEnabled(it) }
    this.enablePulseFade?.let { builder.pulseFadeEnabled(it) }
    this.pulseColor?.let { builder.pulseColor(it) }

    return builder.build()
}

private fun LocationRequestProperties.toMapLibre(): LocationEngineRequest {
    return LocationEngineRequest.Builder(this.interval)
        .setPriority(this.priority.value)
        .setFastestInterval(this.fastestInterval)
        .setDisplacement(this.displacement)
        .setMaxWaitTime(this.maxWaitTime)
        .build()
}

private fun MapboxMap.addImages(context: Context, images: List<Pair<String, Int>>?) {
    images?.let {
        images.mapNotNull { image ->
            val drawable = context.getDrawable(image.second)
            val bitmap = BitmapUtils.getBitmapFromDrawable(drawable)
            bitmap?.let { Pair(image.first, bitmap) }
        }.forEach {
            style!!.addImage(it.first, it.second)
        }
    }
}

private fun MapboxMap.addSources(sources: List<Source>?) {
    sources?.let { sources.forEach { style!!.addSource(it) } }
}

private fun MapboxMap.addLayers(layers: List<Layer>?) {
    layers?.let { layers.forEach { style!!.addLayer(it) } }
}

@Composable
internal fun MapUpdater(
    cameraPosition: CameraPosition,
    cameraPositionCallback: CameraPositionCallback?
) {
    val mapApplier = currentComposer.applier as MapApplier

    fun observeZoom(cameraPosition: CameraPosition) {
        mapApplier.map.addOnScaleListener(object : OnScaleListener {
            override fun onScaleBegin(detector: StandardScaleGestureDetector) {}

            override fun onScale(detector: StandardScaleGestureDetector) {
                cameraPosition.zoom = mapApplier.map.cameraPosition.zoom
            }

            override fun onScaleEnd(detector: StandardScaleGestureDetector) {}
        })
    }

    fun observeCameraPosition(
        cameraPosition: CameraPosition,
        onCameraMoveEnded: (CameraPosition) -> Unit
    ) {
        mapApplier.map.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {}

            override fun onMove(detector: MoveGestureDetector) {
                cameraPosition.trackingMode = CameraTrackingMode.NONE
                cameraPosition.target = mapApplier.map.cameraPosition.target
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {
                onCameraMoveEnded(cameraPosition)
            }
        })
    }

    fun observeBearing(cameraPosition: CameraPosition) {
        mapApplier.map.addOnRotateListener(object : OnRotateListener {
            override fun onRotateBegin(detector: RotateGestureDetector) {}

            override fun onRotate(detector: RotateGestureDetector) {
                cameraPosition.trackingMode = CameraTrackingMode.NONE
                cameraPosition.bearing = mapApplier.map.cameraPosition.bearing
            }

            override fun onRotateEnd(detector: RotateGestureDetector) {}
        })
    }

    fun observeTilt(cameraPosition: CameraPosition) {
        mapApplier.map.addOnShoveListener(object : OnShoveListener {
            override fun onShoveBegin(detector: ShoveGestureDetector) {}

            override fun onShove(detector: ShoveGestureDetector) {
                cameraPosition.tilt = mapApplier.map.cameraPosition.tilt
            }

            override fun onShoveEnd(detector: ShoveGestureDetector) {}
        })
    }

    fun observeIdle(cameraPosition: CameraPosition, onCameraIdle: (CameraPosition) -> Unit) {
        mapApplier.map.addOnCameraIdleListener {
            cameraPosition.target = mapApplier.map.cameraPosition.target
            cameraPosition.bearing = mapApplier.map.cameraPosition.bearing
            cameraPosition.zoom = mapApplier.map.cameraPosition.zoom
            cameraPosition.tilt = mapApplier.map.cameraPosition.tilt
        }
    }

    ComposeNode<MapPropertiesNode, MapApplier>(factory = {
        MapPropertiesNode(mapApplier.map, cameraPosition)
    }, update = {
        observeZoom(cameraPosition)
        observeCameraPosition(cameraPosition) {
            // TODO: This captures 90% of the move, but not the final position..
//            cameraPositionCallback?.onChanged(it)
        }
        observeBearing(cameraPosition)
        observeTilt(cameraPosition)
        observeIdle(cameraPosition) {
            cameraPositionCallback?.onChanged(it)
        }

        update(cameraPosition) {
            this.cameraPosition = it
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition.toMapbox())

            when (cameraPosition.trackingMode) {
                CameraTrackingMode.NONE -> {
                    if (map.locationComponent.isLocationComponentActivated) {
                        map.locationComponent.cameraMode = CameraMode.NONE
                    }

                    when (cameraPosition.motionType) {
                        CameraMotionType.INSTANT -> map.moveCamera(cameraUpdate)

                        CameraMotionType.EASE -> map.easeCamera(
                            cameraUpdate,
                            cameraPosition.animationDurationMs
                        )

                        CameraMotionType.FLY -> map.animateCamera(
                            cameraUpdate,
                            cameraPosition.animationDurationMs
                        )
                    }
                }
                CameraTrackingMode.FOLLOW -> {
                    assert(map.locationComponent.isLocationComponentActivated)
                    map.locationComponent.cameraMode = CameraMode.TRACKING
                    map.locationComponent.renderMode = RenderMode.COMPASS
                }
                CameraTrackingMode.FOLLOW_WITH_BEARING -> {
                    assert(map.locationComponent.isLocationComponentActivated)
                    map.locationComponent.cameraMode = CameraMode.TRACKING_GPS
                    map.locationComponent.renderMode = RenderMode.COMPASS
                }
            }
        }
    })
}

internal class MapPropertiesNode(
    val map: MapboxMap,
    var cameraPosition: CameraPosition
) : MapNode {
    override fun onAttached() {
        when (cameraPosition.trackingMode) {
            CameraTrackingMode.NONE -> {
                if (map.locationComponent.isLocationComponentActivated) {
                    map.locationComponent.cameraMode = CameraMode.NONE
                }
                map.cameraPosition = cameraPosition.toMapbox()
            }
            CameraTrackingMode.FOLLOW -> {
                assert(map.locationComponent.isLocationComponentActivated)
                map.locationComponent.cameraMode = CameraMode.TRACKING
                map.locationComponent.renderMode = RenderMode.COMPASS
            }
            CameraTrackingMode.FOLLOW_WITH_BEARING -> {
                assert(map.locationComponent.isLocationComponentActivated)
                map.locationComponent.cameraMode = CameraMode.TRACKING_GPS
                map.locationComponent.renderMode = RenderMode.COMPASS
            }
        }
    }
}

