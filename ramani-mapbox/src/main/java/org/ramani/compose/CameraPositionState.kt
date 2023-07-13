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

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

data class CameraPosition(
    var target: LatLng? = null,
    var zoom: Double? = null,
    var tilt: Double? = null,
    var bearing: Double? = null,
) : Parcelable {
    constructor(cameraPosition: CameraPosition) : this(
        cameraPosition.target,
        cameraPosition.zoom,
        cameraPosition.tilt,
        cameraPosition.bearing
    )

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(LatLng::class.java.classLoader),
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readValue(Double::class.java.classLoader) as? Double
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(target, flags)
        parcel.writeValue(zoom)
        parcel.writeValue(tilt)
        parcel.writeValue(bearing)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CameraPosition> {
        override fun createFromParcel(parcel: Parcel): CameraPosition {
            return CameraPosition(parcel)
        }

        override fun newArray(size: Int): Array<CameraPosition?> {
            return arrayOfNulls(size)
        }
    }
}

class CameraPositionState(var cameraPosition: CameraPosition = CameraPosition()) {
    companion object {
        val Saver: Saver<CameraPositionState, CameraPosition> = Saver(
            save = { it.cameraPosition },
            restore = { CameraPositionState(it) }
        )
    }
}

@Composable
inline fun rememberCameraPositionState(
    key: String? = null,
    crossinline init: CameraPositionState.() -> Unit = {}
): CameraPositionState = rememberSaveable(key = key, saver = CameraPositionState.Saver) {
    CameraPositionState().apply(init)
}
