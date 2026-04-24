/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2023 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.maplibre.android.geometry.LatLng

/**
 * A state holder for an annotation's center, providing bidirectional synchronisation
 * between user code and the map:
 *
 * - **User → map:** Setting [center] moves the annotation on the map.
 * - **Map → user:** Drag-driven position changes are reflected in [center]
 *   so that composables reading it recompose automatically.
 *
 * Use [rememberCenterState] to create an instance that survives
 * recomposition and configuration changes.
 */
class CenterState(initialCenter: LatLng) {
    var center by mutableStateOf(initialCenter)

    // Called by the drag handler to update the center. Unlike the public
    // setter this is a no-op from the map's perspective (the annotation is
    // already at the new position).
    internal fun updateCenterFromDrag(newCenter: LatLng) {
        center = newCenter
    }

    companion object {
        val Saver: Saver<CenterState, LatLng> = Saver(
            save = { it.center },
            restore = { CenterState(it) }
        )
    }
}

/**
 * Creates and remembers a [CenterState] that survives recomposition
 * and configuration changes.
 *
 * @param center The initial center position.
 */
@Composable
fun rememberCenterState(
    center: LatLng = LatLng()
): CenterState = rememberSaveable(saver = CenterState.Saver) {
    CenterState(center)
}
