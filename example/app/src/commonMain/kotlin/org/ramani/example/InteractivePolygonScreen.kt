/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ramani.compose.CameraPosition
import org.ramani.compose.Circle
import org.ramani.compose.LatLng
import org.ramani.compose.MapLibre
import org.ramani.compose.MapStyle
import org.ramani.compose.Polygon
import org.ramani.compose.rememberCameraPositionState
import org.ramani.compose.rememberPolygonState

private const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"

private val polygonPoints = listOf(
    LatLng(44.986, 10.812),
    LatLng(44.986, 10.807),
    LatLng(44.992, 10.807),
    LatLng(44.992, 10.812),
)

@Composable
fun InteractivePolygonScreen() {
    val polygonState = rememberPolygonState(polygonPoints)
    val cameraPositionState = rememberCameraPositionState(
        CameraPosition(target = polygonState.center, zoom = 15.0)
    )

    val isDefaultStyle = rememberSaveable { mutableStateOf(true) }
    val styleUrl = rememberSaveable { mutableStateOf(DEFAULT_STYLE_URL) }
    val style = MapStyle.Uri(styleUrl.value)

    Box {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MapLibre(
                modifier = Modifier.fillMaxSize(),
                style = style,
                cameraPositionState = cameraPositionState,
            ) {
                polygonState.vertexStates.forEachIndexed { index, state ->
                    Circle(
                        centerState = state,
                        layerId = "vertex_$index",
                        aboveLayerId = "editable_polygon",
                        radius = 10.0F,
                        color = "Blue",
                    )
                    Circle(
                        centerState = state,
                        aboveLayerId = "vertex_$index",
                        radius = 30.0F,
                        color = "Blue",
                        opacity = 0.0F,
                        isDraggable = true,
                    )
                }
                Polygon(
                    state = polygonState,
                    layerId = "editable_polygon",
                    isDraggable = true,
                    draggerImageId = dragIconResource,
                    borderWidth = 4.0F,
                    fillColor = "Yellow",
                    opacity = 0.5F,
                )
            }
        }
        val camPos = cameraPositionState.position
        Text(
            text = "lat: ${camPos.target?.latitude?.fmt(4)}  lng: ${camPos.target?.longitude?.fmt(4)}  zoom: ${camPos.zoom?.fmt(1)}",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    styleUrl.value =
                        if (!isDefaultStyle.value) DEFAULT_STYLE_URL
                        else ApiKeys.MAPLIBRE_STYLE_URL
                    isDefaultStyle.value = !isDefaultStyle.value
                }) {
                Text("Swap style")
            }
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    cameraPositionState.position = cameraPositionState.position.copy(
                        target = polygonState.center,
                        animationDurationMs = 3000,
                    )
                },
            ) {
                Text(text = "Center on polygon")
            }
        }
    }
}
