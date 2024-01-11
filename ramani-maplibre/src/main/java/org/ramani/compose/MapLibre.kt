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
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTargetMarker
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMoveListener
import com.mapbox.mapboxsdk.maps.MapboxMap.OnRotateListener
import com.mapbox.mapboxsdk.maps.MapboxMap.OnScaleListener
import com.mapbox.mapboxsdk.maps.MapboxMap.OnShoveListener
import com.mapbox.mapboxsdk.maps.Style

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
 * @param content The content of the map.
 */
@Composable
fun MapLibre(
    modifier: Modifier,
    styleUrl: String = "https://demotiles.maplibre.org/style.json",
    cameraPosition: CameraPosition = rememberSaveable { CameraPosition() },
    uiSettings: UiSettings = UiSettings(),
    properties: MapProperties = MapProperties(),
    locationRequestProperties: LocationRequestProperties? = null,
    locationStyling: LocationStyling = LocationStyling(),
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
    val currentLocationRequestProperties by rememberUpdatedState(locationRequestProperties)
    val currentLocationStyling by rememberUpdatedState(locationStyling)
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
                currentLocationRequestProperties,
                currentLocationStyling
            )

            map.newComposition(parentComposition, style) {
                CompositionLocalProvider {
                    MapUpdater(cameraPosition = currentCameraPosition)
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
    locationRequestProperties: LocationRequestProperties?,
    locationStyling: LocationStyling,
) {
    if (locationRequestProperties == null) return

    val locationActivationOptions = LocationComponentActivationOptions
        .builder(context, style)
        .locationComponentOptions(locationStyling.toMapLibre(context))
        .useDefaultLocationEngine(true)
        .locationEngineRequest(locationRequestProperties.toMapLibre())
        .build()
    this.locationComponent.activateLocationComponent(locationActivationOptions)

    if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    ) {
        this.locationComponent.isLocationComponentEnabled = true
    }
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
        .setFastestInterval(this.fastestInterval)
        .setPriority(this.priority.value)
        .build()
}

@Composable
internal fun MapUpdater(cameraPosition: CameraPosition) {
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

    fun observeCameraPosition(cameraPosition: CameraPosition) {
        mapApplier.map.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {}

            override fun onMove(detector: MoveGestureDetector) {
                cameraPosition.target = mapApplier.map.cameraPosition.target
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {}
        })
    }

    fun observeBearing(cameraPosition: CameraPosition) {
        mapApplier.map.addOnRotateListener(object : OnRotateListener {
            override fun onRotateBegin(detector: RotateGestureDetector) {}

            override fun onRotate(detector: RotateGestureDetector) {
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

    fun observeIdle(cameraPosition: CameraPosition) {
        mapApplier.map.addOnCameraIdleListener {
            cameraPosition.zoom = mapApplier.map.cameraPosition.zoom
            cameraPosition.target = mapApplier.map.cameraPosition.target
            cameraPosition.bearing = mapApplier.map.cameraPosition.bearing
            cameraPosition.tilt = mapApplier.map.cameraPosition.tilt
        }
    }

    ComposeNode<MapPropertiesNode, MapApplier>(factory = {
        MapPropertiesNode(mapApplier.map, cameraPosition)
    }, update = {
        observeZoom(cameraPosition)
        observeCameraPosition(cameraPosition)
        observeBearing(cameraPosition)
        observeTilt(cameraPosition)
        observeIdle(cameraPosition)

        update(cameraPosition) {
            this.cameraPosition = it
            map.cameraPosition = cameraPosition.toMapbox()
        }
    })
}

internal class MapPropertiesNode(
    val map: MapboxMap,
    var cameraPosition: CameraPosition
) : MapNode {
    override fun onAttached() {
        map.cameraPosition = cameraPosition.toMapbox()
    }
}

internal fun CameraPosition.toMapbox(): com.mapbox.mapboxsdk.camera.CameraPosition {
    val builder = com.mapbox.mapboxsdk.camera.CameraPosition.Builder()

    target?.let { builder.target(it) }
    zoom?.let { builder.zoom(it) }
    tilt?.let { builder.tilt(it) }
    bearing?.let { builder.bearing(it) }

    return builder.build()
}
