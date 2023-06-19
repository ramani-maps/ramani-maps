package org.ramani.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng

class CameraPositionState(
    val cameraPosition: CameraPosition = CameraPosition.Builder()
        .target(LatLng(0.0, 0.0))
        .zoom(0.0)
        .tilt(0.0)
        .bearing(0.0)
        .build()
) {
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
