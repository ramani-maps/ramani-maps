package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions

@Composable
@MapLibreComposable
fun Fill(
    points: MutableList<MutableList<LatLng>>,
    fillColor: String = "Transparent",
    opacity: Float = 1.0f,
    isDraggable: Boolean = false,
) {
    val mapApplier = currentComposer.applier as MapApplier

    ComposeNode<FillNode, MapApplier>(factory = {
        val fillOptions = FillOptions()
            .withLatLngs(points)
            .withFillColor(fillColor)
            .withFillOpacity(opacity)
            .withDraggable(isDraggable)
        val fill = mapApplier.fillManager.create(fillOptions)

        FillNode(mapApplier.fillManager, fill)
    }, update = {
        set(points) {
            fill.latLngs = points
            fillManager.update(fill)
        }

        set(fillColor) {
            fill.fillColor = fillColor
            fillManager.update(fill)
        }

        set(opacity) {
            fill.fillOpacity = opacity
            fillManager.update(fill)
        }
    })
}
