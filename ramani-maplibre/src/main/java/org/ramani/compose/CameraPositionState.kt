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
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.parcelize.Parcelize
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
    var target: LatLng? = null,
    var zoom: Double? = null,
    var tilt: Double? = null,
    var bearing: Double? = null,
    var motionType: CameraMotionType = FLY,
    var animationDurationMs: Int = 1000,
) : Parcelable {
    constructor(cameraPosition: CameraPosition) : this(
        cameraPosition.target,
        cameraPosition.zoom,
        cameraPosition.tilt,
        cameraPosition.bearing,
        cameraPosition.motionType,
        cameraPosition.animationDurationMs,
    )
}
