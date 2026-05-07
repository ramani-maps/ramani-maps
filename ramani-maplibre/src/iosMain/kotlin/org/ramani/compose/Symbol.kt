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

import MapLibre.MLNPointFeature
import MapLibre.MLNShapeSource
import MapLibre.MLNSymbolStyleLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSExpression
import platform.Foundation.NSNumber
import platform.Foundation.NSUUID

actual val DefaultMarkerImage: Any = Unit

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun Symbol(
    centerState: CenterState,
    size: Float,
    color: String,
    isDraggable: Boolean,
    layerId: String?,
    aboveLayerId: String?,
    belowLayerId: String?,
    imageId: Any?,
    imageAnchor: String,
    imageOffset: Array<Float>,
    imageRotation: Float?,
    text: String?,
    textAnchor: String,
    textJustify: String,
    textOffset: Array<Float>,
    textColor: String,
    textHaloColor: String,
    textHaloWidth: Float,
    data: Any?,
    onSymbolDragged: (LatLng) -> Unit,
    onDragFinished: (LatLng) -> Unit,
    onClick: (Any?) -> Unit,
    onLongClick: (Any?) -> Unit,
) {
    val mapApplier = LocalMapApplier.current
    val resolvedLayerId = layerId ?: remember { NSUUID().UUIDString }
    val sourceId = remember(resolvedLayerId) { "source-symbol-$resolvedLayerId" }

    ComposeNode<SymbolNode, MapApplier>(factory = {
        val feature = MLNPointFeature()
        feature.setCoordinate(centerState.center.toCLLocationCoordinate2D())

        val source = MLNShapeSource(identifier = sourceId, shape = feature, options = null)
        val layer = MLNSymbolStyleLayer(identifier = resolvedLayerId, source = source)

        text?.let {
            layer.text = NSExpression.expressionForConstantValue(it)
            layer.textColor = NSExpression.expressionForConstantValue(parseColor(textColor))
            layer.textHaloColor = NSExpression.expressionForConstantValue(parseColor(textHaloColor))
            layer.textHaloWidth = NSExpression.expressionForConstantValue(NSNumber(float = textHaloWidth))
            layer.textFontSize = NSExpression.expressionForConstantValue(NSNumber(float = size))
            layer.textAnchor = NSExpression.expressionForConstantValue(textAnchor)
            layer.textJustification = NSExpression.expressionForConstantValue(textJustify)
        }

        imageRotation?.let {
            layer.iconRotation = NSExpression.expressionForConstantValue(NSNumber(float = it))
        }

        layer.iconAllowsOverlap = NSExpression.expressionForConstantValue(true)

        mapApplier.addSourceAndLayer(source, layer)

        SymbolNode(
            mapApplier = mapApplier,
            sourceId = sourceId,
            layerId = resolvedLayerId,
            isDraggable = isDraggable,
            onSymbolDragged = onSymbolDragged,
            onDragFinished = onDragFinished,
            centerState = centerState,
        )
    }, update = {
        set(centerState.center) {
            val feature = MLNPointFeature()
            feature.setCoordinate(centerState.center.toCLLocationCoordinate2D())
            (mapApplier.style?.sourceWithIdentifier(sourceId) as? MLNShapeSource)?.shape = feature
        }

        set(text) {
            (mapApplier.style?.layerWithIdentifier(resolvedLayerId) as? MLNSymbolStyleLayer)
                ?.text = NSExpression.expressionForConstantValue(text ?: "")
        }

        set(color) {
            (mapApplier.style?.layerWithIdentifier(resolvedLayerId) as? MLNSymbolStyleLayer)
                ?.iconColor = NSExpression.expressionForConstantValue(parseColor(color))
        }

        set(imageRotation) {
            (mapApplier.style?.layerWithIdentifier(resolvedLayerId) as? MLNSymbolStyleLayer)
                ?.iconRotation = NSExpression.expressionForConstantValue(imageRotation?.let { NSNumber(float = it) })
        }

        set(isDraggable) { this.isDraggable = isDraggable }
        set(onSymbolDragged) { this.onSymbolDragged = onSymbolDragged }
        set(onDragFinished) { this.onDragFinished = onDragFinished }
    })
}
