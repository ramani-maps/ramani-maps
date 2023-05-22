package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions

@Composable
@MapLibreComposable
fun PolyLine(
    points: MutableList<LatLng>,
    color: String,
    lineWidth: Float,
    isDraggable: Boolean = false
) {
    val mapApplier = currentComposer.applier as MapApplier

    ComposeNode<PolyLineNode, MapApplier>(factory = {
        val lineOptions = LineOptions()
            .withLatLngs(points)
            .withLineColor(color)
            .withLineWidth(lineWidth)
            .withDraggable(isDraggable)
        val polyLine = mapApplier.lineManager.create(lineOptions)

        PolyLineNode(mapApplier.lineManager, polyLine)
    }, update = {
        set(points) {
            polyLine.latLngs = points
            lineManager.update(polyLine)
        }

        set(color) {
            polyLine.lineColor = color
            lineManager.update(polyLine)
        }
    })
}
