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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.maps.MapboxMap

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
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    content: (@Composable @MapLibreComposable () -> Unit)? = null,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    val map = rememberMapViewWithLifecycle()
    val currentCameraPositionState by rememberUpdatedState(cameraPositionState)
    val currentContent by rememberUpdatedState(content)
    val parentComposition = rememberCompositionContext()

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { map })
    LaunchedEffect(Unit) {
        disposingComposition {
            map.newComposition(parentComposition, style = map.awaitMap().awaitStyle(apiKey)) {
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
            map.cameraPosition = cameraPositionState.cameraPosition.toMapbox()
        }
    })
}

internal class MapPropertiesNode(
    val map: MapboxMap,
    var cameraPositionState: CameraPositionState
) : MapNode {
    override fun onAttached() {
        map.cameraPosition = cameraPositionState.cameraPosition.toMapbox()
    }
}

internal fun org.ramani.compose.CameraPosition.toMapbox(): CameraPosition {
    val builder = CameraPosition.Builder()

    target?.let { builder.target(it) }
    zoom?.let { builder.zoom(it) }
    tilt?.let { builder.tilt(it) }
    bearing?.let { builder.bearing(it) }

    return builder.build()
}
