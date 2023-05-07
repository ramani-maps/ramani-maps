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
    draggable: Boolean,
    color: String,
    borderColor: String = "Black",
    borderWidth: Float = 0.0f,
    imageId: Int? = null,
    imageSize: Float = 0.0f,
    text: String? = null,
    onCenterChanged: (LatLng) -> Unit = {}
) {

    var draggableCenterState = remember {
        mutableStateOf(center)
    }

    UpdateCenter(coord = center, centerUpdated = { draggableCenterState.value = it })

    // invisible circle, this is draggable
    Circle(center = draggableCenterState.value,
        radius = 30.0f,
        draggable = draggable,
        color = "Transparent",
        borderColor = borderColor,
        borderWidth = 0.0f,
        onCenterDragged = {
            onCenterChanged(it)
        }, onDragFinished = {
            draggableCenterState.value = center
        }
    )

    // display circle, this is not dragged
    Circle(center = center,
        radius = radius,
        draggable = false,
        color = color,
        borderColor = borderColor,
        borderWidth = borderWidth,
        onCenterDragged = {

        }
    )

    if (imageId != null || text != null) {
        Symbol(
            center = center,
            color = "Black",
            draggable = false,
            imageId = imageId,
            size = imageSize,
            text = null,
        )
    }
}
