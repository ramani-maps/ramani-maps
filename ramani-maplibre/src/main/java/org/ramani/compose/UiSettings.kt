/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2024 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package org.ramani.compose

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class UiSettings(var compassMargins: CompassMargins = CompassMargins()) : Parcelable {
    constructor(uiSettings: UiSettings) : this(uiSettings.compassMargins)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UiSettings

        return compassMargins == other.compassMargins
    }

    override fun hashCode(): Int {
        return compassMargins.hashCode()
    }
}

@Parcelize
class CompassMargins(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0) :
    Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompassMargins

        if (left != other.left) return false
        if (top != other.top) return false
        if (right != other.right) return false
        return bottom == other.bottom
    }

    override fun hashCode(): Int {
        var result = left
        result = 31 * result + top
        result = 31 * result + right
        result = 31 * result + bottom
        return result
    }
}
