/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.ramani.compose.CameraMode
import org.ramani.compose.CameraPosition
import org.ramani.compose.LatLng
import org.ramani.compose.LocationRequestProperties
import org.ramani.compose.LocationStyling
import org.ramani.compose.MapLibre
import org.ramani.compose.MapStyle
import org.ramani.compose.RenderMode
import org.ramani.compose.UserLocation
import org.ramani.compose.rememberCameraPositionState

@Composable
fun LocationScreen() {
    val locationProperties = remember { mutableStateOf<LocationRequestProperties?>(null) }
    val cameraPositionState = rememberCameraPositionState(CameraPosition(zoom = 14.0))
    val userLocation = remember { mutableStateOf(UserLocation()) }
    val cameraMode = rememberSaveable { mutableStateOf(CameraMode.TRACKING) }
    val renderMode = rememberSaveable { mutableStateOf(RenderMode.COMPASS) }

    val permissionLauncher = rememberLocationPermissionLauncher { granted ->
        if (granted) {
            locationProperties.value = LocationRequestProperties()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch()
    }

    LaunchedEffect(userLocation.value) {
        val loc = userLocation.value
        if (loc.latitude != 0.0 || loc.longitude != 0.0) {
            cameraPositionState.position = cameraPositionState.position.copy(
                target = LatLng(loc.latitude, loc.longitude)
            )
        }
    }

    val style = MapStyle.Uri(ApiKeys.MAPLIBRE_STYLE_URL)

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
                    pulseColor = 0xFFFFFF00.toInt(),
                ),
                userLocation = userLocation,
                cameraMode = cameraMode,
                renderMode = renderMode.value,
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
                    cameraMode.value = if (cameraMode.value == CameraMode.NONE) {
                        CameraMode.TRACKING_GPS
                    } else {
                        CameraMode.NONE
                    }
                },
            ) {
                if (cameraMode.value == CameraMode.NONE) {
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
                            userLocation.value.longitude,
                        )
                    )
                },
            ) {
                Text(text = "Center on device location")
            }
        }
    }
}
