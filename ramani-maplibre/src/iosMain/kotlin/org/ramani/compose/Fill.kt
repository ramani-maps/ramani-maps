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

import MapLibre.MLNFillStyleLayer
import MapLibre.MLNPolygonFeature
import MapLibre.MLNShapeSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import platform.CoreLocation.CLLocationCoordinate2D
import platform.Foundation.NSExpression
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID

@OptIn(ExperimentalForeignApi::class)
internal fun createPolygonFeature(points: List<LatLng>): MLNPolygonFeature {
    return memScoped {
        val coords = allocArray<CLLocationCoordinate2D>(points.size)
        points.forEachIndexed { i, point ->
            coords[i].latitude = point.latitude
            coords[i].longitude = point.longitude
        }
        MLNPolygonFeature.polygonWithCoordinates(coords, count = points.size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun Fill(
    points: List<LatLng>,
    fillColor: String,
    opacity: Float,
    layerId: String?,
    aboveLayerId: String?,
    belowLayerId: String?,
    isDraggable: Boolean,
) {
    val mapApplier = LocalMapApplier.current
    val resolvedLayerId = layerId ?: remember { NSUUID().UUIDString }
    val sourceId = remember(resolvedLayerId) { "source-fill-$resolvedLayerId" }

    ComposeNode<FillNode, MapApplier>(factory = {
        val feature = createPolygonFeature(points)
        val source = MLNShapeSource(identifier = sourceId, shape = feature, options = null)
        val layer = MLNFillStyleLayer(identifier = resolvedLayerId, source = source)
        layer.fillColor = NSExpression.expressionForConstantValue(parseColor(fillColor))
        layer.fillOpacity = NSExpression.expressionForConstantValue(NSNumber(float = opacity))

        mapApplier.addSourceAndLayer(source, layer)

        FillNode(mapApplier, sourceId, resolvedLayerId)
    }, update = {
        set(points) {
            val feature = createPolygonFeature(points)
            (mapApplier.style?.sourceWithIdentifier(sourceId) as? MLNShapeSource)?.shape = feature
        }

        set(fillColor) {
            (mapApplier.style?.layerWithIdentifier(resolvedLayerId) as? MLNFillStyleLayer)
                ?.fillColor = NSExpression.expressionForConstantValue(parseColor(fillColor))
        }

        set(opacity) {
            (mapApplier.style?.layerWithIdentifier(resolvedLayerId) as? MLNFillStyleLayer)
                ?.fillOpacity = NSExpression.expressionForConstantValue(NSNumber(float = opacity))
        }
    })
}
