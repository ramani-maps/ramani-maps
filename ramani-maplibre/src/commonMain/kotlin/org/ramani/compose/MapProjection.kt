/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.json.JsonObject
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry

interface MapProjection {
    fun toScreenLocation(latLng: LatLng): ScreenPoint

    fun fromScreenLocation(point: ScreenPoint): LatLng

    /**
     * Returns the rendered features at [point] (in screen pixels), sorted by render order with the
     * feature drawn in front first. The query is widened to a square of half-size [radiusPx] around
     * the point to give finger-tap tolerance.
     *
     * @param layerIds if non-null, restricts the query to features in these style layers.
     */
    fun queryRenderedFeatures(
        point: ScreenPoint,
        radiusPx: Float = 8f,
        layerIds: Set<String>? = null,
    ): List<Feature<Geometry, JsonObject?>>

    fun setLayerVisibility(layerId: String, visible: Boolean)
}

val LocalMapProjection = staticCompositionLocalOf<MapProjection> {
    error("MapProjection not provided. Map composables must be used inside a MapLibre { } block.")
}
