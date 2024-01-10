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

class UiSettings(var compassMargins: CompassMargins = CompassMargins()) : Parcelable {
    constructor (uiSettings: UiSettings) : this(uiSettings.compassMargins)

    constructor(parcel: Parcel) : this(parcel.readParcelable<CompassMargins>(CompassMargins::class.java.classLoader)!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(compassMargins, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UiSettings

        return compassMargins == other.compassMargins
    }

    override fun hashCode(): Int {
        return compassMargins.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<UiSettings> {
        override fun createFromParcel(parcel: Parcel): UiSettings {
            return UiSettings(parcel)
        }

        override fun newArray(size: Int): Array<UiSettings?> {
            return arrayOfNulls(size)
        }
    }
}

class CompassMargins(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0) :
    Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(left)
        parcel.writeInt(top)
        parcel.writeInt(right)
        parcel.writeInt(bottom)
    }

    override fun describeContents(): Int {
        return 0
    }

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

    companion object CREATOR : Parcelable.Creator<CompassMargins> {
        override fun createFromParcel(parcel: Parcel): CompassMargins {
            return CompassMargins(parcel)
        }

        override fun newArray(size: Int): Array<CompassMargins?> {
            return arrayOfNulls(size)
        }
    }
}
