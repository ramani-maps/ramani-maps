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
import androidx.annotation.FloatRange
import com.mapbox.mapboxsdk.constants.MapboxConstants

class MapProperties(
    @FloatRange(
        from = MapboxConstants.MINIMUM_ZOOM.toDouble(),
        to = MapboxConstants.MAXIMUM_ZOOM.toDouble()
    ) var maxZoom: Double? = null,
) : Parcelable {
    constructor(mapProperties: MapProperties) : this(mapProperties.maxZoom)

    constructor(parcel: Parcel) : this(parcel.readValue(Double::class.java.classLoader) as? Double)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(maxZoom)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapProperties

        return maxZoom == other.maxZoom
    }

    override fun hashCode(): Int {
        return maxZoom?.hashCode() ?: 0
    }

    companion object CREATOR : Parcelable.Creator<MapProperties> {
        override fun createFromParcel(parcel: Parcel): MapProperties {
            return MapProperties(parcel)
        }

        override fun newArray(size: Int): Array<MapProperties?> {
            return arrayOfNulls(size)
        }
    }
}
