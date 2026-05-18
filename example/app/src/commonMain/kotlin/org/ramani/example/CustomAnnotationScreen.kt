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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.ramani.compose.CameraPosition
import org.ramani.compose.LatLng
import org.ramani.compose.MapLibre
import org.ramani.compose.MapObserver
import org.ramani.compose.rememberCameraPositionState

@Composable
fun CustomAnnotationScreen() {
    val cameraPositionState = rememberCameraPositionState(
        CameraPosition(target = LatLng(44.989, 10.809), zoom = 6.0)
    )
    val progress = remember { mutableFloatStateOf(0.0f) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        MapLibre(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            MapObserver(onMapRotated = {
                progress.floatValue = (it / 360).toFloat()
            })

            ProgressCircle(
                center = LatLng(44.989, 10.809),
                radius = 25.0f,
                progress = ProgressPercent((progress.floatValue * 100).toInt()),
                borderWidth = 5.0f,
                fillColor = "Orange",
                borderColor = "Blue",
                indicatorTextSize = 15.0f,
            )
        }
    }
}
