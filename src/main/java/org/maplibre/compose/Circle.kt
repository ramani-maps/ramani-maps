package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.Circle
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions
import com.mapbox.mapboxsdk.plugins.annotation.OnCircleDragListener

@Composable
@MapLibreComposable
fun Circle(
    center: LatLng,
    radius: Float,
    isDraggable: Boolean,
    color: String,
    borderColor: String = "Black",
    borderWidth: Float = 0.0f,
    onCenterDragged: (LatLng) -> Unit,
    onDragFinished: (LatLng) -> Unit = {}
) {
    val mapApplier = currentComposer.applier as? MapApplier


    ComposeNode<CircleNode, MapApplier>(factory = {
        val circleManager =
            CircleManager(mapApplier?.mapView!!, mapApplier?.map!!, mapApplier?.style!!)

        val circleOptions =
            CircleOptions().withCircleRadius(radius)
                .withLatLng(center).withDraggable(isDraggable).withCircleStrokeColor(borderColor)
                .withCircleStrokeWidth(borderWidth)

        val circle = circleManager.create(circleOptions)
        circleManager.addDragListener(object : OnCircleDragListener {
            override fun onAnnotationDragStarted(annotation: Circle?) {
            }

            override fun onAnnotationDrag(annotation: Circle?) {
                onCenterDragged(annotation?.latLng!!)
            }

            override fun onAnnotationDragFinished(annotation: Circle?) {
                onDragFinished(annotation?.latLng!!)
            }

        })
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
    }) {
    }
}
