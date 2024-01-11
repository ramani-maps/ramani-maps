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
import androidx.annotation.ColorInt

/**
 * @property accuracyAlpha Opacity of the accuracy view between 0 (transparent) and 1 (opaque).
 * @property accuracyColor Color of the accuracy view.
 * @property enablePulse Enable the location pulsing circle.
 * @property enablePulseFade Enable the fading of the pulsing circle.
 * @property pulseColor Color of the pulsing circle.
 */
class LocationStyling(
    var accuracyAlpha: Float? = null,
    @ColorInt var accuracyColor: Int? = null,
    var enablePulse: Boolean? = null,
    var enablePulseFade: Boolean? = null,
    @ColorInt var pulseColor: Int? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Float::class.java.classLoader) as? Float,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        parcel.readValue(Int::class.java.classLoader) as? Int
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(accuracyAlpha)
        parcel.writeValue(accuracyColor)
        parcel.writeValue(enablePulse)
        parcel.writeValue(enablePulseFade)
        parcel.writeValue(pulseColor)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocationStyling

        if (accuracyAlpha != other.accuracyAlpha) return false
        if (accuracyColor != other.accuracyColor) return false
        if (enablePulse != other.enablePulse) return false
        if (enablePulseFade != other.enablePulseFade) return false
        return pulseColor == other.pulseColor
    }

    override fun hashCode(): Int {
        var result = accuracyAlpha?.hashCode() ?: 0
        result = 31 * result + (accuracyColor ?: 0)
        result = 31 * result + (enablePulse?.hashCode() ?: 0)
        result = 31 * result + (enablePulseFade?.hashCode() ?: 0)
        result = 31 * result + (pulseColor ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<LocationStyling> {
        override fun createFromParcel(parcel: Parcel): LocationStyling {
            return LocationStyling(parcel)
        }

        override fun newArray(size: Int): Array<LocationStyling?> {
            return arrayOfNulls(size)
        }
    }
}
