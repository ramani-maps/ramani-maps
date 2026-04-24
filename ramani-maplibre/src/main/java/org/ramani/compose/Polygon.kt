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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.graphics.minus
import androidx.core.graphics.plus
import org.maplibre.android.geometry.LatLng
import kotlin.math.atan2

@Composable
private fun AzimuthCalculator(
    posA: LatLng,
    posB: LatLng,
    refPos: LatLng,
    onAzimuthChanged: (Float) -> Unit
) {
    val mapApplier = LocalMapApplier.current
    val projection = mapApplier.map.projection

    val localA = projection.toScreenLocation(posA)
    val localB = projection.toScreenLocation(posB)
    val localRef = projection.toScreenLocation(refPos)

    val diff = localB - localA
    val diffRef = localB - localRef

    val refAzimuth = atan2(diffRef.y, diffRef.x)
    val azimuth = atan2(diff.y, diff.x)
    onAzimuthChanged(azimuth - refAzimuth)
}

@Composable
private fun VertexDragger(
    draggedCenter: LatLng,
    vertices: List<LatLng>,
    onCenterAndVerticesChanged: (LatLng, List<LatLng>) -> Unit
) {
    if (vertices.isEmpty()) {
        return
    }

    val mapApplier = LocalMapApplier.current
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
    layerId: String,
    aboveLayerId: String,
    imageId: Int? = null,
    azimuth: Float,
    onCenterChanged: (LatLng) -> Unit = {},
    onVerticesChanged: (List<LatLng>) -> Unit = {},
    onAzimuthChanged: (Float) -> Unit = {},
) {
    val polygonDragHandleCoord = remember {
        mutableStateOf(LatLng())
    }

    val azimuthHandleCoord = remember {
        mutableStateOf(LatLng())
    }

    val inputDragCoord = remember {
        mutableStateOf(LatLng())
    }

    val inputAzimuthCoord = remember {
        mutableStateOf(LatLng())
    }

    val isDragActive = remember {
        mutableStateOf(false)
    }

    val isAzimuthDragActive = remember {
        mutableStateOf(false)
    }

    val startAzimuth = remember {
        mutableStateOf(0.0f)
    }

    val startAzimuthRefPos = remember {
        mutableStateOf(LatLng())
    }

    if (!isAzimuthDragActive.value) {
        azimuthHandleCoord.value = polygonDragHandleCoord.value
    }

    val counter = remember {
        mutableStateOf(0)
    }

    VertexDragger(
        draggedCenter = inputDragCoord.value,
        vertices = vertices,
        onCenterAndVerticesChanged = { center, vertices ->
            polygonDragHandleCoord.value = center
            if (isDragActive.value) {
                onVerticesChanged(vertices)
            }
            onCenterChanged(center)
        })

    AzimuthCalculator(
        posA = inputAzimuthCoord.value,
        posB = polygonDragHandleCoord.value,
        startAzimuthRefPos.value,
        onAzimuthChanged = {
            if (isAzimuthDragActive.value) {
                onAzimuthChanged(it + startAzimuth.value)
            }
        })

    imageId?.let {
        CircleWithItem(
            center = polygonDragHandleCoord.value,
            radius = 20.0f,
            isDraggable = false,
            color = "Transparent",
            borderColor = "Black",
            borderWidth = 1.0f,
            imageId = imageId,
            itemSize = 1.0f,
            layerId = "${layerId}_drag_icon",
            aboveLayerId = aboveLayerId,
        )
    }

    Circle(
        center = azimuthHandleCoord.value,
        radius = 50.0f,
        isDraggable = true,
        color = "Transparent",
        layerId = "${layerId}_rotation_handle",
        aboveLayerId = aboveLayerId,
        onCenterDragged = {
            if (!isAzimuthDragActive.value && counter.value > 0) {
                startAzimuth.value = azimuth
                startAzimuthRefPos.value = it
                isAzimuthDragActive.value = true
            }
            counter.value = counter.value + 1
            inputAzimuthCoord.value = it
            azimuthHandleCoord.value = it
        },
        onDragFinished = {
            isAzimuthDragActive.value = false
            counter.value = 0
        }
    )

    Circle(
        center = polygonDragHandleCoord.value,
        radius = 20.0f,
        isDraggable = true,
        color = "Transparent",
        layerId = "${layerId}_drag_handle",
        aboveLayerId = "${layerId}_rotation_handle",
        onCenterDragged = {
            isDragActive.value = true
            inputDragCoord.value = it
        },
        onDragFinished = {
            isDragActive.value = false
        })
}

@Composable
fun Polygon(
    vertices: List<LatLng>,
    azimuth: Float = 0F,
    draggerImageId: Int? = null,
    fillColor: String = "Transparent",
    borderWidth: Float = 1.0F,
    borderColor: String = "Black",
    opacity: Float = 1.0F,
    layerId: String? = null,
    isDraggable: Boolean = false,
    onCenterChanged: (LatLng) -> Unit = {},
    onVerticesChanged: (List<LatLng>) -> Unit = {},
    onAzimuthChanged: (Float) -> Unit = {},
) {
    val mapApplier = LocalMapApplier.current
    val resolvedLayerId = layerId ?: remember { java.util.UUID.randomUUID().toString() }

    val visualTopLayerId = if (borderWidth > 0) "${resolvedLayerId}_border" else "${resolvedLayerId}_fill"
    val topLayerId = if (isDraggable) "${resolvedLayerId}_drag_handle" else visualTopLayerId
    mapApplier.registerLayerAlias(resolvedLayerId, topLayerId)

    val borderPath = vertices.toMutableList().apply { this.add(this[0]) }

    Fill(
        points = borderPath,
        fillColor = fillColor,
        opacity = opacity,
        isDraggable = false,
        layerId = "${resolvedLayerId}_fill",
    )

    if (borderWidth > 0) {
        Polyline(
            points = borderPath,
            color = borderColor,
            lineWidth = borderWidth,
            layerId = "${resolvedLayerId}_border",
            aboveLayerId = "${resolvedLayerId}_fill",
        )
    }

    if (isDraggable) {
        PolygonDragHandle(
            vertices = vertices,
            layerId = resolvedLayerId,
            aboveLayerId = visualTopLayerId,
            imageId = draggerImageId,
            azimuth = azimuth,
            onCenterChanged = { onCenterChanged(it) },
            onVerticesChanged = { onVerticesChanged(it) },
            onAzimuthChanged = onAzimuthChanged,
        )
    }
}
