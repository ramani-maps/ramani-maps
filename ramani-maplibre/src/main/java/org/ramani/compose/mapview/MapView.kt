package org.ramani.compose.mapview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.ramani.compose.camera.MapViewCamera
import org.ramani.compose.CameraPosition
import org.ramani.compose.CameraPositionCallback
import org.ramani.compose.LocationRequestProperties
import org.ramani.compose.MapLibre
import org.ramani.compose.MapLibreComposable

@Composable
fun MapView(
    modifier: Modifier = Modifier.fillMaxSize(),
    styleUrl: String,
    camera: MapViewCamera = MapViewCamera.Default,
    onCameraChanged: (MapViewCamera) -> Unit = {},
    content: (@Composable @MapLibreComposable () -> Unit)? = null
) {

    val cameraCallback = object : CameraPositionCallback {
        override fun onChanged(cameraPosition: CameraPosition) {
            onCameraChanged(MapViewCamera.fromCameraPosition(cameraPosition))
        }
    }

    MapLibre(
        modifier,
        styleUrl,
        cameraPosition = camera.cameraPosition(),
        locationRequestProperties = LocationRequestProperties(),
        cameraPositionCallback = cameraCallback
    ) {
        content?.invoke()
    }
}

@Preview
@Composable
fun MapViewPreview() {
    MapView(styleUrl = "https://demotiles.maplibre.org/style.json")
}