package org.ramani.example.interactive_polygon

import android.Manifest
import android.graphics.Color
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.ramani.compose.CameraPosition
import org.ramani.compose.MapStyle
import org.ramani.compose.LocationRequestProperties
import org.ramani.compose.LocationStyling
import org.ramani.compose.MapLibre
import org.ramani.compose.rememberCameraPositionState

@Composable
fun LocationScreen() {
    val context = LocalContext.current
    val locationProperties = rememberSaveable { mutableStateOf(LocationRequestProperties()) }
    val cameraPositionState = rememberCameraPositionState(CameraPosition(zoom = 14.0))
    val userLocation = rememberSaveable { mutableStateOf(Location(null)) }
    val cameraMode = rememberSaveable { mutableIntStateOf(CameraMode.TRACKING) }
    val renderMode = rememberSaveable { mutableIntStateOf(RenderMode.COMPASS) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
                || permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (granted) {
            locationProperties.value = LocationRequestProperties()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    LaunchedEffect(userLocation.value) {
        val loc = userLocation.value
        if (loc.latitude != 0.0 || loc.longitude != 0.0) {
            cameraPositionState.position = cameraPositionState.position.copy(
                target = LatLng(loc.latitude, loc.longitude)
            )
        }
    }

    val style = MapStyle.Uri(context.getString(R.string.maplibre_style_url))

    Box {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MapLibre(
                modifier = Modifier.fillMaxSize(),
                style = style,
                cameraPositionState = cameraPositionState,
                locationRequestProperties = locationProperties.value,
                locationStyling = LocationStyling(
                    enablePulse = true,
                    pulseColor = Color.YELLOW,
                ),
                userLocation = userLocation,
                cameraMode = cameraMode,
                renderMode = renderMode.intValue,
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    renderMode.value =
                        if (renderMode.value == RenderMode.COMPASS) RenderMode.NORMAL else RenderMode.COMPASS
                }
            ) {
                if (renderMode.value == RenderMode.COMPASS) {
                    Text("RenderMode.NORMAL")
                } else {
                    Text("RenderMode.COMPASS")
                }
            }
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    cameraMode.intValue = if (cameraMode.intValue == CameraMode.NONE) {
                        CameraMode.TRACKING_GPS
                    } else {
                        CameraMode.NONE
                    }
                },
            ) {
                if (cameraMode.intValue == CameraMode.NONE) {
                    Text(text = "Follow")
                } else {
                    Text(text = "Stop following")
                }
            }
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    cameraPositionState.position = cameraPositionState.position.copy(
                        target = LatLng(
                            userLocation.value.latitude,
                            userLocation.value.longitude
                        )
                    )
                },
            ) {
                Text(text = "Center on device location")
            }
        }
    }
}
