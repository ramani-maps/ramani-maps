package org.ramani.example.location

import android.Manifest
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.Style
import org.ramani.compose.CameraPosition
import org.ramani.compose.LocationRequestProperties
import org.ramani.compose.LocationStyling
import org.ramani.compose.MapLibre
import org.ramani.example.location.ui.theme.LocationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val locationPropertiesState: MutableState<LocationRequestProperties?> = mutableStateOf(null)
        requestPermissions(locationPropertiesState)

        setContent {
            LocationTheme {
                val locationProperties = rememberSaveable { locationPropertiesState }
                val cameraPosition = rememberSaveable { mutableStateOf(CameraPosition()) }
                val userLocation = rememberSaveable { mutableStateOf(Location(null)) }

                Box {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MapLibre(
                            modifier = Modifier.fillMaxSize(),
                            styleBuilder = Style.Builder()
                                .fromUri(resources.getString(R.string.maplibre_style_url)),
                            cameraPosition = cameraPosition.value,
                            locationRequestProperties = locationProperties.value,
                            locationStyling = LocationStyling(
                                enablePulse = true,
                                pulseColor = Color.YELLOW,
                            ),
                            userLocation = userLocation,
                            cameraMode = CameraMode.TRACKING_GPS,
                        )
                    }
                    Button(
                        modifier = Modifier.align(Alignment.BottomCenter),
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

    private fun requestPermissions(locationPropertiesState: MutableState<LocationRequestProperties?>) {
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

                else -> {
                    locationPropertiesState.value = null
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
