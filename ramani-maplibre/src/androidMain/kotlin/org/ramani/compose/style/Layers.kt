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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.ramani.compose.LocalMapApplier
import org.ramani.compose.MapApplier
import org.ramani.compose.MapNode
import org.maplibre.android.style.layers.CircleLayer as MlCircleLayer
import org.maplibre.android.style.layers.FillLayer as MlFillLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.LineLayer as MlLineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer as MlRasterLayer
import org.maplibre.android.style.layers.SymbolLayer as MlSymbolLayer

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
            val layer = MlCircleLayer(id, sourceId).apply {
                filter?.let { setFilter(it.toMapLibre()) }
                val props = buildList {
                    radius?.let { add(PropertyFactory.circleRadius(it.toMapLibre())) }
                    color?.let { add(PropertyFactory.circleColor(it.toMapLibre())) }
                    opacity?.let { add(PropertyFactory.circleOpacity(it.toMapLibre())) }
                }
                if (props.isNotEmpty()) setProperties(*props.toTypedArray())
            }
            LayerNode(mapApplier.style, layer).apply { attach() }
        },
        update = {
            set(radius) { expr ->
                layer?.let { l ->
                    (l as MlCircleLayer).setProperties(
                        PropertyFactory.circleRadius(expr?.toMapLibre() ?: return@set)
                    )
                }
            }
            set(color) { expr ->
                layer?.let { l ->
                    (l as MlCircleLayer).setProperties(
                        PropertyFactory.circleColor(expr?.toMapLibre() ?: return@set)
                    )
                }
            }
            set(opacity) { expr ->
                layer?.let { l ->
                    (l as MlCircleLayer).setProperties(
                        PropertyFactory.circleOpacity(expr?.toMapLibre() ?: return@set)
                    )
                }
            }
        },
    )
}

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
            val layer = MlFillLayer(id, sourceId).apply {
                filter?.let { setFilter(it.toMapLibre()) }
                val props = buildList {
                    color?.let { add(PropertyFactory.fillColor(it.toMapLibre())) }
                    opacity?.let { add(PropertyFactory.fillOpacity(it.toMapLibre())) }
                }
                if (props.isNotEmpty()) setProperties(*props.toTypedArray())
            }
            LayerNode(mapApplier.style, layer).apply { attach() }
        },
        update = {
            set(color) { expr ->
                layer?.let { l ->
                    (l as MlFillLayer).setProperties(
                        PropertyFactory.fillColor(expr?.toMapLibre() ?: return@set)
                    )
                }
            }
            set(opacity) { expr ->
                layer?.let { l ->
                    (l as MlFillLayer).setProperties(
                        PropertyFactory.fillOpacity(expr?.toMapLibre() ?: return@set)
                    )
                }
            }
        },
    )
}

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
            val layer = MlLineLayer(id, sourceId).apply {
                sourceLayer?.let { this.sourceLayer = it }
                filter?.let { setFilter(it.toMapLibre()) }
                val props = buildList {
                    color?.let { add(PropertyFactory.lineColor(it.toMapLibre())) }
                    width?.let { add(PropertyFactory.lineWidth(it.toMapLibre())) }
                    opacity?.let { add(PropertyFactory.lineOpacity(it.toMapLibre())) }
                }
                if (props.isNotEmpty()) setProperties(*props.toTypedArray())
            }
            LayerNode(mapApplier.style, layer).apply { attach() }
        },
        update = {},
    )
}

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
            val layer = MlSymbolLayer(id, sourceId).apply {
                filter?.let { setFilter(it.toMapLibre()) }
                val props = buildList {
                    textField?.let { add(PropertyFactory.textField(it.toMapLibre())) }
                    textSize?.let { add(PropertyFactory.textSize(it.toMapLibre())) }
                    textColor?.let { add(PropertyFactory.textColor(it.toMapLibre())) }
                    if (textIgnorePlacement) add(PropertyFactory.textIgnorePlacement(true))
                    if (textAllowOverlap) add(PropertyFactory.textAllowOverlap(true))
                }
                if (props.isNotEmpty()) setProperties(*props.toTypedArray())
            }
            LayerNode(mapApplier.style, layer).apply { attach() }
        },
        update = {},
    )
}

@Composable
actual fun RasterStyleLayer(
    id: String,
    sourceId: String,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<LayerNode, MapApplier>(
        factory = {
            LayerNode(mapApplier.style, MlRasterLayer(id, sourceId)).apply { attach() }
        },
        update = {},
    )
}

internal class LayerNode(
    val style: androidx.compose.runtime.MutableState<org.maplibre.android.maps.Style?>,
    val layer: Layer?,
) : MapNode {
    fun attach() {
        layer ?: return
        style.value?.let { s ->
            s.getLayer(layer.id)?.let { existing -> s.removeLayer(existing) }
            s.addLayer(layer)
        }
    }

    override fun onRemoved() {
        layer?.let { style.value?.removeLayer(it) }
    }

    override fun onCleared() {
        layer?.let { style.value?.removeLayer(it) }
    }

    fun reattach() {
        try {
            layer?.let { style.value?.addLayer(it) }
        } catch (_: IllegalStateException) {
            // Style is being replaced
        }
    }
}
