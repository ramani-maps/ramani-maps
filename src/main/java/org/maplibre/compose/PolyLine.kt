package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.Circle
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions
import com.mapbox.mapboxsdk.plugins.annotation.Line
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.plugins.annotation.OnCircleDragListener
import com.mapbox.mapboxsdk.plugins.annotation.OnLineDragListener

@Composable
@MapLibreComposable
fun PolyLine(points : MutableList<LatLng>, color: String, lineWidth: Float, draggable: Boolean = false) {

    val mapApplier = currentComposer.applier as? MapApplier

    ComposeNode<PolyLineNode, MapApplier>(factory = {

        val lineManager = LineManager(mapApplier?.mapView!!, mapApplier?.map!!, mapApplier?.style!!)

        val lineOptions = LineOptions().withLatLngs(points).withLineColor(color).withLineWidth(lineWidth).withDraggable(draggable)

        val polyLine = lineManager.create(lineOptions)
        lineManager.addDragListener(object : OnLineDragListener {
            override fun onAnnotationDragStarted(annotation: Line?) {
            }

            override fun onAnnotationDrag(annotation: Line?) {
            }
            override fun onAnnotationDragFinished(annotation: Line?) {
            }

        })
        PolyLineNode(lineManager, polyLine) {

        }
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