package org.maplibre.compose

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.graphics.plus
import com.mapbox.mapboxsdk.geometry.LatLng

@Composable
fun VerticeDragger(
    draggedCenter: LatLng,
    points: MutableList<LatLng>,
    onCenterAndVerticesChanged: (LatLng, MutableList<LatLng>) -> Unit
) {

    if (points.size <= 0) {
        return
    }

    var mapApplier = currentComposer.applier as? MapApplier
    val projection = mapApplier?.map!!.projection

    val currentCenter = PointF()

    for (coord in points) {
        currentCenter.x += projection.toScreenLocation(coord).x
        currentCenter.y += projection.toScreenLocation(coord).y
    }

    currentCenter.x = currentCenter.x / points.size
    currentCenter.y = currentCenter.y / points.size

    val newCenter = projection.toScreenLocation(draggedCenter)

    val draggedPixels: PointF = PointF(newCenter.x - currentCenter.x, newCenter.y - currentCenter.y)

    val draggedVertices = mutableListOf<LatLng>()

    for (coord in points) {
        val currentLoc = projection.toScreenLocation(coord)
        draggedVertices.add(projection.fromScreenLocation(currentLoc + draggedPixels))
    }

    onCenterAndVerticesChanged(projection.fromScreenLocation(currentCenter), draggedVertices)
}

@MapLibreComposable
@Composable
fun Polygon(
    vertices: MutableList<MutableList<LatLng>>,
    fillColor: String = "Transparent",
    opacity: Float = 1.0f,
    draggable: Boolean = false,
    onPointsChanged: (MutableList<MutableList<LatLng>>) -> Unit,
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


    if (draggable) {
        VerticeDragger(
            draggedCenter = inputDragCoord.value,
            points = vertices.first(),
            onCenterAndVerticesChanged = { center, vertices ->
                polygonDragHandleCoord.value = center
                if (dragActive.value) {
                    onPointsChanged(mutableListOf(vertices))
                }
            })

        Circle(
            center = polygonDragHandleCoord.value,
            radius = 30.0f,
            draggable = true,
            color = "Transparent",
            onCenterDragged = {
                dragActive.value = true
                inputDragCoord.value = it
            },
            onDragFinished = {
                dragActive.value = false
            })
    }

    Fill(
        points = vertices,
        fillColor = fillColor,
        opacity = opacity,
        draggable = false,
        onPointsChanged = onPointsChanged
    )
}