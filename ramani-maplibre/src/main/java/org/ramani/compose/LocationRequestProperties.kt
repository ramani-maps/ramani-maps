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
import org.ramani.compose.LocationPriority.PRIORITY_BALANCED_POWER_ACCURACY
import org.ramani.compose.LocationPriority.PRIORITY_HIGH_ACCURACY
import org.ramani.compose.LocationPriority.PRIORITY_LOW_POWER
import org.ramani.compose.LocationPriority.PRIORITY_NO_POWER

/**
 * @property PRIORITY_HIGH_ACCURACY Request the most accurate location.
 * @property PRIORITY_BALANCED_POWER_ACCURACY Request coarse location that is battery optimized.
 * @property PRIORITY_LOW_POWER Request coarse ~10km accuracy location.
 * @property PRIORITY_NO_POWER Request passive location (no locations will be returned unless a different client requests location updates).
 */
enum class LocationPriority(val value: Int) {
    PRIORITY_HIGH_ACCURACY(0),
    PRIORITY_BALANCED_POWER_ACCURACY(1),
    PRIORITY_LOW_POWER(2),
    PRIORITY_NO_POWER(3)
}

class LocationRequestProperties(
    var priority: LocationPriority = PRIORITY_HIGH_ACCURACY,
    var interval: Long = 1000L,
    var fastestInterval: Long = 0L,
    var displacement: Float = 0F,
    var maxWaitTime: Long = 0L
) : Parcelable {
    constructor(locationRequestProperties: LocationRequestProperties) : this(
        locationRequestProperties.priority,
        locationRequestProperties.interval,
        locationRequestProperties.fastestInterval,
        locationRequestProperties.displacement,
        locationRequestProperties.maxWaitTime,
    )

    constructor(parcel: Parcel) : this(
        LocationPriority.valueOf(parcel.readInt().toString()),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readFloat(),
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(priority.value)
        parcel.writeLong(interval)
        parcel.writeLong(fastestInterval)
        parcel.writeFloat(displacement)
        parcel.writeLong(maxWaitTime)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocationRequestProperties

        if (priority != other.priority) return false
        if (interval != other.interval) return false
        if (fastestInterval != other.fastestInterval) return false
        if (displacement != other.displacement) return false
        return maxWaitTime == other.maxWaitTime
    }

    override fun hashCode(): Int {
        var result = priority.hashCode()
        result = 31 * result + interval.hashCode()
        result = 31 * result + fastestInterval.hashCode()
        result = 31 * result + displacement.hashCode()
        result = 31 * result + maxWaitTime.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<LocationRequestProperties> {
        override fun createFromParcel(parcel: Parcel): LocationRequestProperties {
            return LocationRequestProperties(parcel)
        }

        override fun newArray(size: Int): Array<LocationRequestProperties?> {
            return arrayOfNulls(size)
        }
    }
}
