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

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.graphics.minus
import androidx.core.graphics.plus
import com.mapbox.mapboxsdk.geometry.LatLng

@Composable
private fun VertexDragger(
    draggedCenter: LatLng,
    vertices: List<LatLng>,
    onCenterAndVerticesChanged: (LatLng, List<LatLng>) -> Unit
) {
    if (vertices.isEmpty()) {
        return
    }

    val mapApplier = currentComposer.applier as MapApplier
    val projection = mapApplier.map.projection

    var currentCenter = PointF()

    vertices.forEach { currentCenter += projection.toScreenLocation(it) }

    currentCenter.x = currentCenter.x / vertices.size
    currentCenter.y = currentCenter.y / vertices.size

    val newCenter = projection.toScreenLocation(draggedCenter)
    val draggedPixels: PointF = newCenter - currentCenter

    val draggedVertices = vertices.map { vertex ->
        projection.fromScreenLocation(projection.toScreenLocation(vertex) + draggedPixels)
    }

    onCenterAndVerticesChanged(projection.fromScreenLocation(currentCenter), draggedVertices)
}

@Composable
private fun PolygonDragHandle(
    vertices: List<LatLng>,
    imageId: Int? = null,
    zIndexDragHandle: Int = 0,
    onCenterChanged: (LatLng) -> Unit = {},
    onVerticesChanged: (List<LatLng>) -> Unit = {}
) {
    val polygonDragHandleCoord = remember {
        mutableStateOf(LatLng())
    }

    val inputDragCoord = remember {
        mutableStateOf(LatLng())
    }

    val dragActive = remember {
        mutableStateOf(false)
    }

    VertexDragger(
        draggedCenter = inputDragCoord.value,
        vertices = vertices,
        onCenterAndVerticesChanged = { center, vertices ->
            polygonDragHandleCoord.value = center
            if (dragActive.value) {
                onVerticesChanged(vertices)
            }
            onCenterChanged(center)
        })

    Circle(
        center = polygonDragHandleCoord.value,
        radius = 30.0f,
        isDraggable = true,
        color = "Transparent",
        zIndex = zIndexDragHandle,
        onCenterDragged = {
            dragActive.value = true
            inputDragCoord.value = it
        },
        onDragFinished = {
            dragActive.value = false
        })

    imageId?.let {
        Symbol(
            center = polygonDragHandleCoord.value,
            size = 3.0f,
            color = "Black",
            isDraggable = false,
            imageId = imageId,
            zIndex = zIndexDragHandle,
        )
    }
}

@MapLibreComposable
@Composable
fun Polygon(
    vertices: List<LatLng>,
    draggerImageId: Int? = null,
    fillColor: String = "Transparent",
    borderWidth: Float = 1.0F,
    borderColor: String = "Black",
    opacity: Float = 1.0f,
    zIndex: Int = 0,
    zIndexDragHandle: Int = zIndex + 1,
    isDraggable: Boolean = false,
    onCenterChanged: (LatLng) -> Unit = {},
    onVerticesChanged: (List<LatLng>) -> Unit = {},
) {
    val borderPath = vertices.toMutableList().apply { this.add(this[0]) }
    Fill(
        points = borderPath,
        fillColor = fillColor,
        opacity = opacity,
        isDraggable = false,
        zIndex = zIndex
    )
    if (borderWidth > 0) {
        Polyline(
            points = borderPath,
            color = borderColor,
            lineWidth = borderWidth,
            zIndex = zIndex + 1,
        )
    }
    if (isDraggable) {
        PolygonDragHandle(
            vertices = vertices,
            imageId = draggerImageId,
            zIndexDragHandle = zIndexDragHandle,
            onCenterChanged = { onCenterChanged(it) },
            onVerticesChanged = { onVerticesChanged(it) },
        )
    }
}
