package org.ramani.example.location

import android.Manifest
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.Style
import org.ramani.compose.CameraPosition
import org.ramani.compose.LocationRequestProperties
import org.ramani.compose.LocationStyling
import org.ramani.compose.MapLibre
import org.ramani.example.location.ui.theme.LocationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val locationPropertiesState: MutableState<LocationRequestProperties> =
            mutableStateOf(LocationRequestProperties())
        requestPermissions(locationPropertiesState)

        setContent {
            LocationTheme {
                val locationProperties = rememberSaveable { locationPropertiesState }
                val cameraPosition =
                    rememberSaveable { mutableStateOf(CameraPosition(zoom = 14.0)) }
                val userLocation = rememberSaveable { mutableStateOf(Location(null)) }
                val cameraMode = rememberSaveable { mutableIntStateOf(CameraMode.TRACKING) }
                val renderMode = rememberSaveable { mutableIntStateOf(RenderMode.COMPASS) }

                val styleBuilder =
                    Style.Builder().fromUri(resources.getString(R.string.maplibre_style_url))

                Box {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MapLibre(
                            modifier = Modifier.fillMaxSize(),
                            styleBuilder = styleBuilder,
                            cameraPosition = cameraPosition.value,
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
                                cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                                    this.target = LatLng(
                                        userLocation.value.latitude,
                                        userLocation.value.longitude
                                    )
                                }
                            },
                        ) {
                            Text(text = "Center on device location")
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions(locationPropertiesState: MutableState<LocationRequestProperties>) {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    locationPropertiesState.value = LocationRequestProperties()
                }

                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    locationPropertiesState.value = LocationRequestProperties()
                }
            }
        }

        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}
