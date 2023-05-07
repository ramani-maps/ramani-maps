package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.Fill
import com.mapbox.mapboxsdk.plugins.annotation.FillManager
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions
import com.mapbox.mapboxsdk.plugins.annotation.OnFillDragListener

@Composable
@MapLibreComposable
fun Fill(
    points: MutableList<MutableList<LatLng>>,
    fillColor: String = "Transparent",
    opacity: Float = 1.0f,
    isDraggable: Boolean = false,
    onVericesChanged: (MutableList<MutableList<LatLng>>) -> Unit,
) {

    val mapApplier = currentComposer.applier as? MapApplier

    ComposeNode<FillNode, MapApplier>(factory = {

        val fillManager = FillManager(mapApplier?.mapView!!, mapApplier?.map!!, mapApplier?.style!!)

        val fillOptions =
            FillOptions().withLatLngs(points).withFillColor(fillColor).withFillOpacity(opacity)
                .withDraggable(isDraggable)
        val fill = fillManager.create(fillOptions)



        fillManager.addDragListener(object : OnFillDragListener {
            override fun onAnnotationDragStarted(annotation: Fill?) {
            }

            override fun onAnnotationDrag(annotation: Fill?) {


                for (coord in annotation?.latLngs!!) {
                    println(mapApplier.map.projection.toScreenLocation(coord.first()).x)
                }


                onVericesChanged(annotation?.latLngs!!)
            }

            override fun onAnnotationDragFinished(annotation: Fill?) {
            }

        })

        FillNode(fillManager, fill) {

        }
    }, update = {
        set(points) {
            fill.latLngs = points
            fillManager.update(fill)
        }
        set(fillColor) {
            fill.fillColor = fillColor
            fillManager.update(fill)
        }
    })
}
