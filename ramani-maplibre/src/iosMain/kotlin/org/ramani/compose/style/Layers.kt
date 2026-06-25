/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose.style

import MapLibre.MLNCircleStyleLayer
import MapLibre.MLNFillStyleLayer
import MapLibre.MLNLineStyleLayer
import MapLibre.MLNRasterStyleLayer
import MapLibre.MLNSource
import MapLibre.MLNSymbolStyleLayer
import MapLibre.MLNVectorStyleLayer
import platform.Foundation.NSNumber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import kotlinx.cinterop.ExperimentalForeignApi
import org.ramani.compose.LocalMapApplier
import org.ramani.compose.MapApplier
import org.ramani.compose.MapNode

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CircleStyleLayer(
    id: String,
    sourceId: String,
    filter: Expression?,
    radius: Expression?,
    color: Expression?,
    opacity: Expression?,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<LayerNode, MapApplier>(
        factory = {
            val source = mapApplier.style?.sourceWithIdentifier(sourceId)
            val layer = MLNCircleStyleLayer(identifier = id, source = source!!)
            filter?.let { layer.predicate = it.toNSPredicate() }
            radius?.let { layer.circleRadius = it.toNSExpression() }
            color?.let { layer.circleColor = it.toNSExpression() }
            opacity?.let { layer.circleOpacity = it.toNSExpression() }
            LayerNode(mapApplier, id).also {
                mapApplier.style?.addLayer(layer)
            }
        },
        update = {
            set(radius) { expr ->
                (mapApplier.style?.layerWithIdentifier(id) as? MLNCircleStyleLayer)
                    ?.circleRadius = expr?.toNSExpression() ?: return@set
            }
            set(color) { expr ->
                (mapApplier.style?.layerWithIdentifier(id) as? MLNCircleStyleLayer)
                    ?.circleColor = expr?.toNSExpression() ?: return@set
            }
            set(opacity) { expr ->
                (mapApplier.style?.layerWithIdentifier(id) as? MLNCircleStyleLayer)
                    ?.circleOpacity = expr?.toNSExpression() ?: return@set
            }
        },
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun FillStyleLayer(
    id: String,
    sourceId: String,
    filter: Expression?,
    color: Expression?,
    opacity: Expression?,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<LayerNode, MapApplier>(
        factory = {
            val source = mapApplier.style?.sourceWithIdentifier(sourceId)
            val layer = MLNFillStyleLayer(identifier = id, source = source!!)
            filter?.let { layer.predicate = it.toNSPredicate() }
            color?.let { layer.fillColor = it.toNSExpression() }
            opacity?.let { layer.fillOpacity = it.toNSExpression() }
            LayerNode(mapApplier, id).also {
                mapApplier.style?.addLayer(layer)
            }
        },
        update = {
            set(color) { expr ->
                (mapApplier.style?.layerWithIdentifier(id) as? MLNFillStyleLayer)
                    ?.fillColor = expr?.toNSExpression() ?: return@set
            }
            set(opacity) { expr ->
                (mapApplier.style?.layerWithIdentifier(id) as? MLNFillStyleLayer)
                    ?.fillOpacity = expr?.toNSExpression() ?: return@set
            }
        },
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun LineStyleLayer(
    id: String,
    sourceId: String,
    sourceLayer: String?,
    filter: Expression?,
    color: Expression?,
    width: Expression?,
    opacity: Expression?,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<LayerNode, MapApplier>(
        factory = {
            val source = mapApplier.style?.sourceWithIdentifier(sourceId)
            val layer = MLNLineStyleLayer(identifier = id, source = source!!)
            sourceLayer?.let { (layer as MLNVectorStyleLayer).sourceLayerIdentifier = it }
            filter?.let { layer.predicate = it.toNSPredicate() }
            color?.let { layer.lineColor = it.toNSExpression() }
            width?.let { layer.lineWidth = it.toNSExpression() }
            opacity?.let { layer.lineOpacity = it.toNSExpression() }
            LayerNode(mapApplier, id).also {
                mapApplier.style?.addLayer(layer)
            }
        },
        update = {},
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun SymbolStyleLayer(
    id: String,
    sourceId: String,
    filter: Expression?,
    textField: Expression?,
    textSize: Expression?,
    textColor: Expression?,
    textIgnorePlacement: Boolean,
    textAllowOverlap: Boolean,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<LayerNode, MapApplier>(
        factory = {
            val source = mapApplier.style?.sourceWithIdentifier(sourceId)
            val layer = MLNSymbolStyleLayer(identifier = id, source = source!!)
            filter?.let { layer.predicate = it.toNSPredicate() }
            textField?.let { layer.text = it.toNSExpression() }
            textSize?.let { layer.textFontSize = it.toNSExpression() }
            textColor?.let { layer.textColor = it.toNSExpression() }
            if (textIgnorePlacement) {
                layer.textIgnoresPlacement = platform.Foundation.NSExpression.expressionForConstantValue(
                    NSNumber(bool = true)
                )
            }
            if (textAllowOverlap) {
                layer.textAllowsOverlap = platform.Foundation.NSExpression.expressionForConstantValue(
                    NSNumber(bool = true)
                )
            }
            LayerNode(mapApplier, id).also {
                mapApplier.style?.addLayer(layer)
            }
        },
        update = {},
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RasterStyleLayer(
    id: String,
    sourceId: String,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<LayerNode, MapApplier>(
        factory = {
            val source = mapApplier.style?.sourceWithIdentifier(sourceId)
            val layer = MLNRasterStyleLayer(identifier = id, source = source!!)
            LayerNode(mapApplier, id).also {
                mapApplier.style?.addLayer(layer)
            }
        },
        update = {},
    )
}

@OptIn(ExperimentalForeignApi::class)
internal class LayerNode(
    val mapApplier: MapApplier,
    val layerId: String,
) : MapNode {
    fun reattach() {
        // Layer will be re-added when the source composable re-runs after style reload
    }

    override fun onRemoved() {
        mapApplier.style?.let { s ->
            s.layerWithIdentifier(layerId)?.let { s.removeLayer(it) }
        }
    }

    override fun onCleared() {
        onRemoved()
    }
}
