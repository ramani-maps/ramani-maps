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

import android.graphics.PointF
import android.graphics.RectF
import kotlinx.serialization.json.JsonObject
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry

class MapProjectionAndroid(private val map: MapLibreMap) : MapProjection {
    private val projection get() = map.projection

    override fun toScreenLocation(latLng: LatLng): ScreenPoint {
        val point = projection.toScreenLocation(latLng.toMapLibre())
        return ScreenPoint(point.x, point.y)
    }

    override fun fromScreenLocation(point: ScreenPoint): LatLng {
        val latLng = projection.fromScreenLocation(PointF(point.x, point.y))
        return latLng.toCommon()
    }

    override fun queryRenderedFeatures(
        point: ScreenPoint,
        radiusPx: Float,
        layerIds: Set<String>?,
    ): List<Feature<Geometry, JsonObject?>> {
        val rect = RectF(
            point.x - radiusPx,
            point.y - radiusPx,
            point.x + radiusPx,
            point.y + radiusPx,
        )
        val raw = if (layerIds == null) {
            map.queryRenderedFeatures(rect)
        } else {
            map.queryRenderedFeatures(rect, *layerIds.toTypedArray())
        }
        // Round-trip through GeoJSON so the result is the canonical spatial-k Feature, identical
        // across platforms.
        return raw.map { Feature.fromJson(it.toJson()) }
    }
}
