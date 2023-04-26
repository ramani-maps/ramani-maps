package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions

@Composable
@MapLibreComposable
fun Circle(center: LatLng, draggable: Boolean, color: String) {
    val mapApplier = currentComposer.applier as? MapApplier

    ComposeNode<CircleNode, MapApplier>(factory = {
        val circleManager =
            CircleManager(mapApplier?.mapView!!, mapApplier?.map!!, mapApplier?.style!!)

        val circleOptions =
            CircleOptions().withCircleRadius(30.0f)
                .withLatLng(center).withDraggable(draggable)

        val circle = circleManager.create(circleOptions)
        CircleNode(circleManager, circle) {

        }
    }, update = {
        set(center) {
            circle.latLng = center
            circleManager.update(circle)
        }
        set(color) {
            circle.circleColor = color
            circleManager.update(circle)
        }
    }) {}
}
