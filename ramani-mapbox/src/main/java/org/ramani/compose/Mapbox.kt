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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ResourceOptionsManager
import com.mapbox.maps.Style
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
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
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
        cameraOptions = cameraPositionState.cameraPosition.toMapbox(),
        resourceOptions = resourceOptions,
    )
    val mapView = remember { MapView(context, mapInitOptions) }
    mapView.gestures.scrollDecelerationEnabled =
        false // TODO: otherwise, the fling event happens when dragging an annotation
    mapView.getMapboxMap().apply { loadStyleUri(Style.MAPBOX_STREETS) }

    val currentCameraPositionState by rememberUpdatedState(cameraPositionState)
    val currentContent by rememberUpdatedState(content)
    val parentComposition = rememberCompositionContext()

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })
    LaunchedEffect(Unit) {
        disposingComposition {
            mapView.newComposition(parentComposition) {
                CompositionLocalProvider {
                    MapUpdater(cameraPositionState = currentCameraPositionState)
                    currentContent?.invoke()
                }
            }
        }
    }
}

@Composable
internal fun MapUpdater(cameraPositionState: CameraPositionState) {
    val mapApplier = currentComposer.applier as MapApplier

    ComposeNode<MapPropertiesNode, MapApplier>(factory = {
        MapPropertiesNode(mapApplier.map, cameraPositionState)
    }, update = {
        update(cameraPositionState) {
            this.cameraPositionState = it
            map.setCamera(cameraPositionState.cameraPosition.toMapbox())
        }
    })
}

internal class MapPropertiesNode(
    val map: MapboxMap,
    var cameraPositionState: CameraPositionState
) : MapNode {
    override fun onAttached() {
        map.setCamera(cameraPositionState.cameraPosition.toMapbox())
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
