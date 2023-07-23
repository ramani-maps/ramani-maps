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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMoveListener
import com.mapbox.mapboxsdk.maps.MapboxMap.OnRotateListener
import com.mapbox.mapboxsdk.maps.MapboxMap.OnScaleListener
import com.mapbox.mapboxsdk.maps.MapboxMap.OnShoveListener

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

@Composable
fun MapLibre(
    modifier: Modifier,
    apiKey: String,
    cameraPosition: CameraPosition = rememberSaveable { CameraPosition() },
    content: (@Composable @MapLibreComposable () -> Unit)? = null,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    val map = rememberMapViewWithLifecycle()
    val currentCameraPosition by rememberUpdatedState(cameraPosition)
    val currentContent by rememberUpdatedState(content)
    val parentComposition = rememberCompositionContext()

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { map })
    LaunchedEffect(Unit) {
        disposingComposition {
            map.newComposition(parentComposition, style = map.awaitMap().awaitStyle(apiKey)) {
                CompositionLocalProvider {
                    MapUpdater(cameraPosition = currentCameraPosition)
                    currentContent?.invoke()
                }
            }
        }
    }
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
