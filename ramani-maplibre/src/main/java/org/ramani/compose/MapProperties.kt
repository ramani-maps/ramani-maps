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
import org.maplibre.android.constants.MapLibreConstants
import org.maplibre.android.geometry.LatLngBounds
import kotlinx.parcelize.Parcelize

@Parcelize
class MapProperties(
    @FloatRange(
        from = MapLibreConstants.MINIMUM_ZOOM.toDouble(),
        to = MapLibreConstants.MAXIMUM_ZOOM.toDouble()
    ) val maxZoom: Double? = null,
    @FloatRange(
        from = MapLibreConstants.MINIMUM_ZOOM.toDouble(),
        to = MapLibreConstants.MAXIMUM_ZOOM.toDouble()
    ) val minZoom: Double? = null,
    @FloatRange(
        from = MapLibreConstants.MINIMUM_PITCH.toDouble(),
        to = MapLibreConstants.MAXIMUM_PITCH.toDouble()
    ) val maxPitch: Double? = null,
    @FloatRange(
        from = MapLibreConstants.MINIMUM_PITCH.toDouble(),
        to = MapLibreConstants.MAXIMUM_PITCH.toDouble()
    ) val minPitch: Double? = null,
    val latLngBounds: LatLngBounds? = null
) : Parcelable {
    constructor(mapProperties: MapProperties) : this(
       maxZoom = mapProperties.maxZoom,
       minZoom = mapProperties.minZoom,
       maxPitch = mapProperties.maxPitch,
       minPitch = mapProperties.minPitch,
       latLngBounds = mapProperties.latLngBounds
    )

    fun copy(
        @FloatRange(
            from = MapLibreConstants.MINIMUM_ZOOM.toDouble(),
            to = MapLibreConstants.MAXIMUM_ZOOM.toDouble()
        ) maxZoom: Double? = this.maxZoom,
        @FloatRange(
            from = MapLibreConstants.MINIMUM_ZOOM.toDouble(),
            to = MapLibreConstants.MAXIMUM_ZOOM.toDouble()
        ) minZoom: Double? = this.minZoom,
        @FloatRange(
            from = MapLibreConstants.MINIMUM_PITCH.toDouble(),
            to = MapLibreConstants.MAXIMUM_PITCH.toDouble()
        ) maxPitch: Double? = this.maxPitch,
        @FloatRange(
            from = MapLibreConstants.MINIMUM_PITCH.toDouble(),
            to = MapLibreConstants.MAXIMUM_PITCH.toDouble()
        ) minPitch: Double? = this.minPitch,
        latLngBounds: LatLngBounds? = this.latLngBounds
    ): MapProperties {
        return MapProperties(
            maxZoom = maxZoom,
            minZoom = minZoom,
            maxPitch = maxPitch,
            minPitch = minPitch,
            latLngBounds = latLngBounds
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapProperties

        return maxZoom == other.maxZoom &&
            minZoom == other.minZoom &&
            maxPitch == other.maxPitch &&
            minPitch == other.minPitch &&
            latLngBounds == other.latLngBounds
    }

    override fun hashCode(): Int {
        var result = maxZoom?.hashCode() ?: 0
        result = 31 * result + (minZoom?.hashCode() ?: 0)
        result = 31 * result + (maxPitch?.hashCode() ?: 0)
        result = 31 * result + (minPitch?.hashCode() ?: 0)
        result = 31 * result + (latLngBounds?.hashCode() ?: 0)
        return result
    }
}
