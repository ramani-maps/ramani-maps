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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ResourceOptionsManager
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnRotateListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.OnShoveListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.gestures.addOnRotateListener
import com.mapbox.maps.plugin.gestures.addOnScaleListener
import com.mapbox.maps.plugin.gestures.addOnShoveListener
import com.mapbox.maps.plugin.gestures.gestures

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "Ramani-Mapbox Composable")
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class MapboxComposable

@Composable
fun Mapbox(
    modifier: Modifier,
    apiKey: String,
    cameraPosition: CameraPosition = rememberSaveable { CameraPosition() },
    content: (@Composable @MapboxComposable () -> Unit)? = null,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }
    val context = LocalContext.current

    val resourceOptions = ResourceOptionsManager
        .getDefault(context, apiKey)
        .resourceOptions
    val mapInitOptions = MapInitOptions(
        context,
        cameraOptions = cameraPosition.toMapbox(),
        resourceOptions = resourceOptions,
    )
    val mapView = remember { MapView(context, mapInitOptions) }
    mapView.gestures.scrollDecelerationEnabled =
        false // TODO: otherwise, the fling event happens when dragging an annotation
    mapView.getMapboxMap().apply { loadStyleUri(Style.MAPBOX_STREETS) }

    val currentCameraPosition by rememberUpdatedState(cameraPosition)
    val currentContent by rememberUpdatedState(content)
    val parentComposition = rememberCompositionContext()

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })
    LaunchedEffect(Unit) {
        disposingComposition {
            mapView.newComposition(parentComposition) {
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
                cameraPosition.zoom = mapApplier.map.cameraState.zoom
            }

            override fun onScaleEnd(detector: StandardScaleGestureDetector) {}
        })
    }

    fun observeCameraPosition(cameraPosition: CameraPosition) {
        mapApplier.map.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {}

            override fun onMove(detector: MoveGestureDetector): Boolean {
                val center = mapApplier.map.cameraState.center
                cameraPosition.target = LatLng(center.latitude(), center.longitude())
                return false
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {}
        })
    }

    fun observeBearing(cameraPosition: CameraPosition) {
        mapApplier.map.addOnRotateListener(object : OnRotateListener {
            override fun onRotateBegin(detector: RotateGestureDetector) {}

            override fun onRotate(detector: RotateGestureDetector) {
                cameraPosition.bearing = mapApplier.map.cameraState.bearing
            }

            override fun onRotateEnd(detector: RotateGestureDetector) {}
        })
    }

    fun observeTilt(cameraPosition: CameraPosition) {
        mapApplier.map.addOnShoveListener(object : OnShoveListener {
            override fun onShoveBegin(detector: ShoveGestureDetector) {}

            override fun onShove(detector: ShoveGestureDetector) {
                cameraPosition.tilt = mapApplier.map.cameraState.pitch
            }

            override fun onShoveEnd(detector: ShoveGestureDetector) {}
        })
    }

    ComposeNode<MapPropertiesNode, MapApplier>(factory = {
        MapPropertiesNode(mapApplier.map, cameraPosition)
    }, update = {
        observeZoom(cameraPosition)
        observeCameraPosition(cameraPosition)
        observeBearing(cameraPosition)
        observeTilt(cameraPosition)

        update(cameraPosition) {
            this.cameraPosition = it
            map.setCamera(cameraPosition.toMapbox())
        }
    })
}

internal class MapPropertiesNode(
    val map: MapboxMap,
    var cameraPosition: CameraPosition
) : MapNode {
    override fun onAttached() {
        map.setCamera(cameraPosition.toMapbox())
    }
}

internal fun CameraPosition.toMapbox(): CameraOptions {
    val builder = CameraOptions.Builder()

    target?.let { builder.center(Point.fromLngLat(it.longitude, it.latitude)) }
    zoom?.let { builder.zoom(it) }
    tilt?.let { builder.pitch(it) }
    bearing?.let { builder.bearing(it) }

    return builder.build()
}
