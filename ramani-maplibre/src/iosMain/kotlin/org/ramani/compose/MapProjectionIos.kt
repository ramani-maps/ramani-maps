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

import MapLibre.MLNFeatureProtocol
import MapLibre.MLNMapView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.serialization.json.JsonObject
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry
import platform.CoreGraphics.CGPointMake
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
class MapProjectionIos(private val mapView: MLNMapView) : MapProjection {
    override fun toScreenLocation(latLng: LatLng): ScreenPoint {
        val point = mapView.convertCoordinate(latLng.toCLLocationCoordinate2D(), toPointToView = null)
        return point.useContents { ScreenPoint(x.toFloat(), y.toFloat()) }
    }

    override fun fromScreenLocation(point: ScreenPoint): LatLng {
        val coord = mapView.convertPoint(CGPointMake(point.x.toDouble(), point.y.toDouble()), toCoordinateFromView = null)
        return coord.toCommon()
    }

    override fun queryRenderedFeatures(
        point: ScreenPoint,
        radiusPx: Float,
        layerIds: Set<String>?,
    ): List<Feature<Geometry, JsonObject?>> {
        // iOS hit-testing already applies a small tolerance, so radiusPx is not used here.
        val cgPoint = CGPointMake(point.x.toDouble(), point.y.toDouble())
        val features = if (layerIds == null) {
            mapView.visibleFeaturesAtPoint(cgPoint)
        } else {
            mapView.visibleFeaturesAtPoint(cgPoint, inStyleLayersWithIdentifiers = layerIds)
        }
        return features.mapNotNull { feat ->
            val dictionary = (feat as? MLNFeatureProtocol)?.geoJSONDictionary() ?: return@mapNotNull null
            // Round-trip through GeoJSON so the result is the canonical spatial-k Feature, identical
            // across platforms.
            val data = NSJSONSerialization.dataWithJSONObject(dictionary, 0u, null) ?: return@mapNotNull null
            val json = NSString.create(data, NSUTF8StringEncoding) as String? ?: return@mapNotNull null
            Feature.fromJson(json)
        }
    }
}
