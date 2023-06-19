package org.ramani.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.mapbox.mapboxsdk.geometry.LatLng

data class CameraPosition(
    var target: LatLng? = null,
    var zoom: Double? = null,
    var tilt: Double? = null,
    var bearing: Double? = null,
) {
    constructor(cameraPosition: CameraPosition) : this(
        cameraPosition.target,
        cameraPosition.zoom,
        cameraPosition.tilt,
        cameraPosition.bearing
    )
}

class CameraPositionState(var cameraPosition: CameraPosition = CameraPosition()) {
    companion object {
        val Saver: Saver<CameraPositionState, CameraPosition> = Saver(
            save = { it.cameraPosition },
            restore = { CameraPositionState(it) }
        )
    }
}

@Composable
inline fun rememberCameraPositionState(
    key: String? = null,
    crossinline init: CameraPositionState.() -> Unit = {}
): CameraPositionState = rememberSaveable(key = key, saver = CameraPositionState.Saver) {
    CameraPositionState().apply(init)
}
