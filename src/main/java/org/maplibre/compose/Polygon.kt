package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.FillManager
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions
import com.mapbox.mapboxsdk.plugins.annotation.Line
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.plugins.annotation.OnLineDragListener

@Composable
@MapLibreComposable
fun Fill(points : MutableList<MutableList<LatLng>>, fillColor: String = "Transparent", opacity: Float = 1.0f, draggable: Boolean = false) {

    val mapApplier = currentComposer.applier as? MapApplier

    ComposeNode<FillNode, MapApplier>(factory = {

        val fillManager = FillManager(mapApplier?.mapView!!, mapApplier?.map!!, mapApplier?.style!!)

        val fillOptions = FillOptions().withLatLngs(points).withFillColor(fillColor).withFillOpacity(opacity)
        val fill = fillManager.create(fillOptions)
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
