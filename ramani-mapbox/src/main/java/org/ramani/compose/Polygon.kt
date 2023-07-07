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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.mapbox.geojson.Point
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions

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
    val map = mapApplier.map

    var currentCenter = ScreenCoordinate(0.0, 0.0)
    vertices.forEach { currentCenter += map.pixelForCoordinate(it.toPoint()) }

    currentCenter = ScreenCoordinate(
        currentCenter.x / vertices.size,
        currentCenter.y / vertices.size
    )

    val newCenter = map.pixelForCoordinate(draggedCenter.toPoint())
    val draggedPixels = newCenter - currentCenter
    val draggedVertices = vertices
        .map { vertex ->
            val screenVertex = map.pixelForCoordinate(vertex.toPoint())
            val afterDrag = screenVertex + draggedPixels
            map.coordinateForPixel(afterDrag)
        }
        .map { pointToLatLng(it) }

    onCenterAndVerticesChanged(
        pointToLatLng(map.coordinateForPixel(currentCenter)),
        draggedVertices
    )
}

private fun LatLng.toPoint(): Point {
    return Point.fromLngLat(this.longitude, this.latitude)
}

private fun pointToLatLng(point: Point): LatLng {
    return LatLng(point.latitude(), point.longitude())
}

@Composable
private fun PolygonDragHandle(
    vertices: List<LatLng>,
    imageId: Int? = null,
    zIndexDragHandle: Int = 0,
    onCenterChanged: (LatLng) -> Unit = {},
    onVerticesChanged: (List<LatLng>) -> Unit = {}
) {
    val polygonDragHandleCoord = remember { mutableStateOf(LatLng(0.0, 0.0)) }
    val inputDragCoord = remember { mutableStateOf(LatLng(0.0, 0.0)) }
    val dragActive = remember { mutableStateOf(false) }

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
        onDragFinished = { dragActive.value = false })

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

@MapboxComposable
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
    val mapApplier = currentComposer.applier as MapApplier
    val points = listOf(vertices.map { Point.fromLngLat(it.longitude, it.latitude) })

    ComposeNode<PolygonNode, MapApplier>(factory = {
        val polygonManager = mapApplier.getOrCreatePolygonManagerForZIndex(zIndex)
        val polygonOptions = PolygonAnnotationOptions()
            .withPoints(points)
            .withFillColor(fillColor)
            .withFillOpacity(opacity.toDouble())
        val polygon = polygonManager.create(polygonOptions)

        PolygonNode(polygonManager, polygon)
    }, update = {
        set(vertices) {
            polygon.points = listOf(vertices.map { Point.fromLngLat(it.longitude, it.latitude) })
            polygonManager.update(polygon)
        }

        set(fillColor) {
            polygon.fillColorString = fillColor
            polygonManager.update(polygon)
        }

        set(opacity) {
            polygon.fillOpacity = opacity.toDouble()
            polygonManager.update(polygon)
        }
    })

    val borderPath = vertices.toMutableList().apply { this.add(this[0]) }
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
