package org.ramani.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions

@Composable
@MapLibreComposable
fun Circle(
    center: LatLng,
    radius: Float,
    isDraggable: Boolean,
    color: String,
    opacity: Float = 1.0f,
    borderColor: String = "Black",
    borderWidth: Float = 0.0f,
    zIndex: Int = 0,
    onCenterDragged: (LatLng) -> Unit,
    onDragFinished: (LatLng) -> Unit = {}
) {
    val mapApplier = currentComposer.applier as MapApplier

    ComposeNode<CircleNode, MapApplier>(factory = {
        val circleManager = mapApplier.getCircleManagerForZIndex(zIndex)

        val circleOptions = CircleOptions()
            .withCircleRadius(radius)
            .withLatLng(center)
            .withDraggable(isDraggable)
            .withCircleStrokeColor(borderColor)
            .withCircleStrokeWidth(borderWidth)
            .withCircleOpacity(opacity)

        val circle = circleManager.create(circleOptions)

        CircleNode(
            circleManager,
            circle,
            onCircleDragged = { onCenterDragged(it.latLng) },
            onCircleDragStopped = { onDragFinished(it.latLng) },
        )
    }, update = {
        update(onCenterDragged) {
            this.onCircleDragged = { onCenterDragged(it.latLng) }
        }

        update(onDragFinished) {
            this.onCircleDragStopped = { onDragFinished(it.latLng) }
        }

        set(center) {
            circle.latLng = center
            circleManager.update(circle)
        }

        set(color) {
            circle.circleColor = color
            circleManager.update(circle)
        }

        set(radius) {
            circle.circleRadius = radius
            circleManager.update(circle)
        }
    })
}
