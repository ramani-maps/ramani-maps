package org.ramani.compose.mapview

import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mapbox.mapboxsdk.location.engine.LocationEngine
import com.mapbox.mapboxsdk.location.engine.LocationEngineImpl
import org.ramani.compose.camera.MapViewCamera
import org.ramani.compose.CameraPosition
import org.ramani.compose.CameraPositionCallback
import org.ramani.compose.LocationPriority
import org.ramani.compose.LocationRequestProperties
import org.ramani.compose.LocationStyling
import org.ramani.compose.MapLibre
import org.ramani.compose.MapLibreComposable

@Composable
fun MapView(
    modifier: Modifier = Modifier.fillMaxSize(),
    styleUrl: String,
    camera: MapViewCamera = MapViewCamera.Default,
    locationEngine: LocationEngine? = null,
    userLocation: MutableState<Location>? = null,
    onCameraChanged: (MapViewCamera) -> Unit = {},
    content: (@Composable @MapLibreComposable () -> Unit)? = null
) {

    val cameraCallback = object : CameraPositionCallback {
        override fun onChanged(cameraPosition: CameraPosition) {
            onCameraChanged(MapViewCamera.fromCameraPosition(cameraPosition))
        }
    }

    val locationProperties = LocationRequestProperties(
        priority = LocationPriority.PRIORITY_HIGH_ACCURACY,
        interval = 5000L,
        fastestInterval = 1000L,
        displacement = 0F,
        maxWaitTime = 8000L
    )

    val locationStyling = LocationStyling(
        enablePulse = true,
        enablePulseFade = true,
    )

    MapLibre(
        modifier,
        styleUrl,
        cameraPosition = camera.cameraPosition(),
        locationEngine = locationEngine,
        locationRequestProperties = locationProperties,
        locationStyling = locationStyling,
        userLocation = userLocation,
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