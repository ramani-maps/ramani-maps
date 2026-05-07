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

import MapLibre.MLNLineStyleLayer
import MapLibre.MLNPolylineFeature
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
internal fun createPolylineFeature(points: List<LatLng>): MLNPolylineFeature {
    return memScoped {
        val coords = allocArray<CLLocationCoordinate2D>(points.size)
        points.forEachIndexed { i, point ->
            coords[i].latitude = point.latitude
            coords[i].longitude = point.longitude
        }
        MLNPolylineFeature.polylineWithCoordinates(coords, count = points.size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun Polyline(
    points: List<LatLng>,
    color: String,
    lineWidth: Float,
    layerId: String?,
    aboveLayerId: String?,
    belowLayerId: String?,
    isDraggable: Boolean,
    dashType: Array<Float>?,
) {
    val mapApplier = LocalMapApplier.current
    val resolvedLayerId = layerId ?: remember { NSUUID().UUIDString }
    val sourceId = remember(resolvedLayerId) { "source-line-$resolvedLayerId" }

    ComposeNode<PolyLineNode, MapApplier>(factory = {
        val feature = createPolylineFeature(points)
        val source = MLNShapeSource(identifier = sourceId, shape = feature, options = null)
        val layer = MLNLineStyleLayer(identifier = resolvedLayerId, source = source)
        layer.lineColor = NSExpression.expressionForConstantValue(parseColor(color))
        layer.lineWidth = NSExpression.expressionForConstantValue(NSNumber(float = lineWidth))

        dashType?.let { dash ->
            layer.lineDashPattern = NSExpression.expressionForConstantValue(
                dash.map { NSNumber(float = it) }
            )
        }

        mapApplier.addSourceAndLayer(source, layer, aboveLayerId, belowLayerId)

        PolyLineNode(mapApplier, sourceId, resolvedLayerId)
    }, update = {
        set(points) {
            val feature = createPolylineFeature(points)
            (mapApplier.style?.sourceWithIdentifier(sourceId) as? MLNShapeSource)?.shape = feature
        }

        set(color) {
            (mapApplier.style?.layerWithIdentifier(resolvedLayerId) as? MLNLineStyleLayer)
                ?.lineColor = NSExpression.expressionForConstantValue(parseColor(color))
        }

        set(lineWidth) {
            (mapApplier.style?.layerWithIdentifier(resolvedLayerId) as? MLNLineStyleLayer)
                ?.lineWidth = NSExpression.expressionForConstantValue(NSNumber(float = lineWidth))
        }
    })
}
