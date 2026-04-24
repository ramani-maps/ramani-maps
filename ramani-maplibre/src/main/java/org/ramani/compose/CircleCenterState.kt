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
 * A state holder for a circle's center, providing bidirectional synchronisation
 * between user code and the map:
 *
 * - **User → map:** Setting [center] moves the circle on the map.
 * - **Map → user:** Drag-driven position changes are reflected in [center]
 *   so that composables reading it recompose automatically.
 *
 * Use [rememberCircleCenterState] to create an instance that survives
 * recomposition and configuration changes.
 */
class CircleCenterState(initialCenter: LatLng) {
    var center by mutableStateOf(initialCenter)

    // Called by the drag handler to update the center. Unlike the public
    // setter this is a no-op from the map's perspective (the circle is
    // already at the new position).
    internal fun updateCenterFromDrag(newCenter: LatLng) {
        center = newCenter
    }

    companion object {
        val Saver: Saver<CircleCenterState, LatLng> = Saver(
            save = { it.center },
            restore = { CircleCenterState(it) }
        )
    }
}

/**
 * Creates and remembers a [CircleCenterState] that survives recomposition
 * and configuration changes.
 *
 * @param center The initial center position.
 */
@Composable
fun rememberCircleCenterState(
    center: LatLng = LatLng()
): CircleCenterState = rememberSaveable(saver = CircleCenterState.Saver) {
    CircleCenterState(center)
}
