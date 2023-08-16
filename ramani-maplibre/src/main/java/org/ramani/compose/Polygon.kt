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
import kotlin.math.atan2

@Composable
private fun AzimuthCalculator(
    posA: LatLng,
    posB: LatLng,
    refPos: LatLng,
    onAzimuthChanged: (Float) -> Unit
) {
    val mapApplier = currentComposer.applier as MapApplier
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
    zIndexRotationHandle: Int = zIndexDragHandle,
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

    Circle(
        center = polygonDragHandleCoord.value,
        radius = 20.0f,
        isDraggable = true,
        color = "Transparent",
        zIndex = zIndexDragHandle + 2,
        onCenterDragged = {
            isDragActive.value = true
            inputDragCoord.value = it
        },
        onDragFinished = {
            isDragActive.value = false
        })

    Circle(
        center = azimuthHandleCoord.value,
        radius = 50.0f,
        isDraggable = true,
        color = "Transparent",
        zIndex = zIndexRotationHandle,
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
            zIndex = zIndexDragHandle
        )
    }
}

@MapLibreComposable
@Composable
fun Polygon(
    vertices: List<LatLng>,
    azimuth: Float = 0F,
    draggerImageId: Int? = null,
    fillColor: String = "Transparent",
    borderWidth: Float = 1.0F,
    borderColor: String = "Black",
    opacity: Float = 1.0F,
    zIndex: Int = 0,
    zIndexDragHandle: Int = zIndex + 1,
    zIndexRotationHandle: Int = zIndex + 1,
    isDraggable: Boolean = false,
    onCenterChanged: (LatLng) -> Unit = {},
    onVerticesChanged: (List<LatLng>) -> Unit = {},
    onAzimuthChanged: (Float) -> Unit = {},
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
            zIndexRotationHandle = zIndexRotationHandle,
            azimuth = azimuth,
            onCenterChanged = { onCenterChanged(it) },
            onVerticesChanged = { onVerticesChanged(it) },
            onAzimuthChanged = onAzimuthChanged,
        )
    }
}
