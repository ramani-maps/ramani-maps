package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.mapbox.mapboxsdk.geometry.LatLng

@Composable
fun UpdateCenter(coord: LatLng, centerUpdated: (LatLng) -> Unit) {
    centerUpdated(coord)
}

@Composable
fun CircleWithItem(
    center: LatLng,
    radius: Float,
    isDraggable: Boolean,
    color: String,
    borderColor: String = "Black",
    borderWidth: Float = 0.0f,
    opacity: Float = 1.0f,
    imageId: Int? = null,
    itemSize: Float = 0.0f,
    text: String? = null,
    onCenterChanged: (LatLng) -> Unit = {},
    onDragStopped: () -> Unit = {},
) {
    val draggableCenterState = remember { mutableStateOf(center) }

    UpdateCenter(coord = center, centerUpdated = { draggableCenterState.value = it })

    // Invisible circle, this is the draggable
    Circle(
        center = draggableCenterState.value,
        radius = 30.0f,
        isDraggable = isDraggable,
        color = "Transparent",
        borderColor = borderColor,
        borderWidth = 0.0f,
        onCenterDragged = {
            onCenterChanged(it)
        },
        onDragFinished = {
            draggableCenterState.value = center
            onDragStopped()
        },
    )

    // Display circle
    Circle(
        center = center,
        radius = radius,
        isDraggable = false,
        color = color,
        opacity = opacity,
        borderColor = borderColor,
        borderWidth = borderWidth,
        onCenterDragged = {}
    )

    imageId?.let {
        Symbol(
            center = center,
            color = "Black",
            isDraggable = false,
            imageId = imageId,
            size = itemSize,
        )
    }

    text?.let {
        Symbol(
            center = center,
            color = "Black",
            isDraggable = false,
            text = text,
            size = itemSize,
        )
    }
}
