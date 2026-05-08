/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier

/**
 * A composable representing a MapLibre map.
 *
 * @param modifier The modifier applied to the map.
 * @param style The map style definition. Defaults to the MapLibre demo tiles.
 * @param cameraPositionState The state holder for the camera position.
 * @param uiSettings Settings related to the map UI.
 * @param properties Properties being applied to the map.
 * @param locationRequestProperties Properties related to the location marker. If null,
 *        location will not be enabled on the map.
 * @param locationStyling Styling related to the location marker (color, pulse, etc).
 * @param userLocation If set and if the location is enabled, it will be updated to contain
 *        the latest user location as known by the map.
 * @param renderMode Ways the user location can be rendered on the map.
 * @param cameraMode Set specific camera tracking modes as the device location changes.
 * @param onMapClick Callback that is invoked when the map is clicked.
 * @param onMapLongClick Callback that is invoked when the map is long clicked.
 * @param content The content of the map.
 */
@Composable
expect fun MapLibre(
    modifier: Modifier,
    style: MapStyle = MapStyle.Default,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    uiSettings: UiSettings = UiSettings(),
    properties: MapProperties = MapProperties(),
    locationRequestProperties: LocationRequestProperties? = null,
    locationStyling: LocationStyling = LocationStyling(),
    userLocation: MutableState<UserLocation>? = null,
    renderMode: RenderMode = RenderMode.NORMAL,
    cameraMode: MutableState<CameraMode> = mutableStateOf(CameraMode.NONE),
    onMapClick: (LatLng) -> Unit = {},
    onMapLongClick: (LatLng) -> Unit = {},
    content: (@Composable () -> Unit)? = null,
)
