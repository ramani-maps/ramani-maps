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

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.parcelize.Parcelize
import org.maplibre.android.geometry.LatLng
import org.ramani.compose.CameraMotionType.EASE
import org.ramani.compose.CameraMotionType.FLY
import org.ramani.compose.CameraMotionType.INSTANT

/**
 * @property INSTANT Move the camera instantaneously to the new position.
 * @property EASE Gradually move the camera over [CameraPosition.animationDurationMs] milliseconds.
 * @property FLY Move the camera using a transition that evokes a powered flight
 *           over [CameraPosition.animationDurationMs] milliseconds.
 */
@Parcelize
enum class CameraMotionType : Parcelable { INSTANT, EASE, FLY }

@Parcelize
data class CameraPosition(
    val target: LatLng? = null,
    val zoom: Double? = null,
    val tilt: Double? = null,
    val bearing: Double? = null,
    val motionType: CameraMotionType = FLY,
    val animationDurationMs: Int = 1000,
) : Parcelable

/**
 * A state holder for the camera position, providing bidirectional synchronisation
 * between user code and the map:
 *
 * - **User → map:** Setting [position] triggers a camera animation on the map.
 * - **Map → user:** Gesture-driven camera changes (pan, zoom, rotate, tilt) are
 *   reflected in [position] so that composables reading it recompose automatically.
 *
 * Use [rememberCameraPositionState] to create an instance that survives recomposition
 * and configuration changes.
 */
class CameraPositionState(initialPosition: CameraPosition = CameraPosition()) {
    private var _position by mutableStateOf(initialPosition)

    /**
     * The current camera position. Setting this triggers a camera animation on the map.
     * Reading this always returns the latest position, including gesture-driven updates.
     */
    var position: CameraPosition
        get() = _position
        set(value) {
            _position = value
            _moveGeneration++
        }

    // Incremented only on programmatic changes (public setter). MapUpdater watches this
    // to distinguish user-initiated moves (which need animation) from gesture-driven
    // updates (which don't, since the map is already at the right position).
    private var _moveGeneration by mutableIntStateOf(0)
    internal val moveGeneration: Int get() = _moveGeneration

    // Called by gesture and camera-idle listeners to update the position without
    // triggering a redundant camera animation.
    internal fun updatePositionFromMap(
        target: LatLng? = _position.target,
        zoom: Double? = _position.zoom,
        tilt: Double? = _position.tilt,
        bearing: Double? = _position.bearing,
    ) {
        _position = _position.copy(target = target, zoom = zoom, tilt = tilt, bearing = bearing)
    }

    companion object {
        val Saver: Saver<CameraPositionState, CameraPosition> = Saver(
            save = { it.position },
            restore = { CameraPositionState(it) }
        )
    }
}

/**
 * Creates and remembers a [CameraPositionState] that survives recomposition and
 * configuration changes.
 *
 * @param init The initial camera position.
 */
@Composable
fun rememberCameraPositionState(
    init: CameraPosition = CameraPosition()
): CameraPositionState = rememberSaveable(saver = CameraPositionState.Saver) {
    CameraPositionState(init)
}
