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

class MapProperties(
    val maxZoom: Double? = null,
    val minZoom: Double? = null,
    val maxPitch: Double? = null,
    val minPitch: Double? = null,
    val latLngBounds: LatLngBounds? = null,
    val online: Boolean? = null,
) {
    constructor(mapProperties: MapProperties) : this(
       maxZoom = mapProperties.maxZoom,
       minZoom = mapProperties.minZoom,
       maxPitch = mapProperties.maxPitch,
       minPitch = mapProperties.minPitch,
       latLngBounds = mapProperties.latLngBounds,
       online = mapProperties.online,
    )

    fun copy(
        maxZoom: Double? = this.maxZoom,
        minZoom: Double? = this.minZoom,
        maxPitch: Double? = this.maxPitch,
        minPitch: Double? = this.minPitch,
        latLngBounds: LatLngBounds? = this.latLngBounds,
        online: Boolean? = this.online,
    ): MapProperties {
        return MapProperties(
            maxZoom = maxZoom,
            minZoom = minZoom,
            maxPitch = maxPitch,
            minPitch = minPitch,
            latLngBounds = latLngBounds,
            online = online,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is MapProperties) return false

        return maxZoom == other.maxZoom &&
            minZoom == other.minZoom &&
            maxPitch == other.maxPitch &&
            minPitch == other.minPitch &&
            latLngBounds == other.latLngBounds &&
            online == other.online
    }

    override fun hashCode(): Int {
        var result = maxZoom?.hashCode() ?: 0
        result = 31 * result + (minZoom?.hashCode() ?: 0)
        result = 31 * result + (maxPitch?.hashCode() ?: 0)
        result = 31 * result + (minPitch?.hashCode() ?: 0)
        result = 31 * result + (latLngBounds?.hashCode() ?: 0)
        result = 31 * result + (online?.hashCode() ?: 0)
        return result
    }
}
