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

import MapLibre.MLNCircleStyleLayer
import MapLibre.MLNPointFeature
import MapLibre.MLNShapeSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSExpression
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun Circle(
    centerState: CenterState,
    radius: Float,
    isDraggable: Boolean,
    color: String,
    opacity: Float,
    borderColor: String,
    borderWidth: Float,
    layerId: String?,
    aboveLayerId: String?,
    belowLayerId: String?,
    data: Any?,
    onCenterDragged: (LatLng) -> Unit,
    onDragFinished: (LatLng) -> Unit,
    onClick: (Any?) -> Unit,
    onLongClick: (Any?) -> Unit,
) {
    val mapApplier = LocalMapApplier.current
    val resolvedLayerId = layerId ?: remember { NSUUID().UUIDString }
    val sourceId = remember(resolvedLayerId) { "source-circle-$resolvedLayerId" }

    ComposeNode<CircleNode, MapApplier>(factory = {
        val feature = MLNPointFeature()
        feature.setCoordinate(centerState.center.toCLLocationCoordinate2D())

        val source = MLNShapeSource(identifier = sourceId, shape = feature, options = null)
        val layer = MLNCircleStyleLayer(identifier = resolvedLayerId, source = source)
        layer.circleColor = NSExpression.expressionForConstantValue(parseColor(color))
        layer.circleRadius = NSExpression.expressionForConstantValue(NSNumber(float = radius))
        layer.circleOpacity = NSExpression.expressionForConstantValue(NSNumber(float = opacity))
        layer.circleStrokeColor = NSExpression.expressionForConstantValue(parseColor(borderColor))
        layer.circleStrokeWidth = NSExpression.expressionForConstantValue(NSNumber(float = borderWidth))

        mapApplier.addSourceAndLayer(source, layer, aboveLayerId, belowLayerId)

        CircleNode(
            mapApplier = mapApplier,
            sourceId = sourceId,
            layerId = resolvedLayerId,
            isDraggable = isDraggable,
            onCenterDragged = onCenterDragged,
            onDragFinished = onDragFinished,
            centerState = centerState,
        )
    }, update = {
        set(centerState.center) {
            val feature = MLNPointFeature()
            feature.setCoordinate(centerState.center.toCLLocationCoordinate2D())
            (mapApplier.style?.sourceWithIdentifier(sourceId) as? MLNShapeSource)?.shape = feature
        }

        set(color) {
            (mapApplier.style?.layerWithIdentifier(resolvedLayerId) as? MLNCircleStyleLayer)
                ?.circleColor = NSExpression.expressionForConstantValue(parseColor(color))
        }

        set(radius) {
            (mapApplier.style?.layerWithIdentifier(resolvedLayerId) as? MLNCircleStyleLayer)
                ?.circleRadius = NSExpression.expressionForConstantValue(NSNumber(float = radius))
        }

        set(opacity) {
            (mapApplier.style?.layerWithIdentifier(resolvedLayerId) as? MLNCircleStyleLayer)
                ?.circleOpacity = NSExpression.expressionForConstantValue(NSNumber(float = opacity))
        }

        set(isDraggable) { this.isDraggable = isDraggable }
        set(onCenterDragged) { this.onCenterDragged = onCenterDragged }
        set(onDragFinished) { this.onDragFinished = onDragFinished }
    })
}
