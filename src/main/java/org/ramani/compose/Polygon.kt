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
    vertices: MutableList<LatLng>,
    onCenterAndVerticesChanged: (LatLng, MutableList<LatLng>) -> Unit
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
    }.toMutableList()

    onCenterAndVerticesChanged(projection.fromScreenLocation(currentCenter), draggedVertices)
}

@Composable
private fun PolygonDragHandle(
    vertices: MutableList<LatLng>,
    imageId: Int? = null,
    onCenterChanged: (LatLng) -> Unit = {},
    onVerticesChanged: (MutableList<LatLng>) -> Unit = {}
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
        zIndex = 1,
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
            imageId = imageId
        )
    }
}

@MapLibreComposable
@Composable
fun Polygon(
    vertices: MutableList<LatLng>,
    draggerImageId: Int? = null,
    fillColor: String = "Transparent",
    opacity: Float = 1.0f,
    isDraggable: Boolean = false,
    onVerticesChanged: (MutableList<MutableList<LatLng>>) -> Unit,
) {
    Fill(
        points = vertices,
        fillColor = fillColor,
        opacity = opacity,
        isDraggable = false,
    )
    if (isDraggable) {
        PolygonDragHandle(
            vertices = vertices,
            imageId = draggerImageId,
            onVerticesChanged = {
                onVerticesChanged(mutableListOf(it))
            })
    }
}
