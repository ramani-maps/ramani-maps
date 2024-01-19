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

import android.os.Parcelable
import androidx.annotation.FloatRange
import com.mapbox.mapboxsdk.constants.MapboxConstants
import kotlinx.parcelize.Parcelize

@Parcelize
class MapProperties(
    @FloatRange(
        from = MapboxConstants.MINIMUM_ZOOM.toDouble(),
        to = MapboxConstants.MAXIMUM_ZOOM.toDouble()
    ) var maxZoom: Double? = null,
) : Parcelable {
    constructor(mapProperties: MapProperties) : this(mapProperties.maxZoom)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapProperties

        return maxZoom == other.maxZoom
    }

    override fun hashCode(): Int {
        return maxZoom?.hashCode() ?: 0
    }
}
