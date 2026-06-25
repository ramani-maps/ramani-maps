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
import androidx.compose.runtime.MutableState
import org.ramani.compose.LocalMapApplier
import org.ramani.compose.MapApplier
import org.ramani.compose.MapNode
import org.maplibre.android.maps.Style
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
    val createLayer: () -> Layer = {
        MlCircleLayer(id, sourceId).apply {
            filter?.let { setFilter(it.toMapLibre()) }
            val props = buildList {
                radius?.let { add(PropertyFactory.circleRadius(it.toMapLibre())) }
                color?.let { add(PropertyFactory.circleColor(it.toMapLibre())) }
                opacity?.let { add(PropertyFactory.circleOpacity(it.toMapLibre())) }
            }
            if (props.isNotEmpty()) setProperties(*props.toTypedArray())
        }
    }
    ComposeNode<LayerNode, MapApplier>(
        factory = { LayerNode(mapApplier.style, id, createLayer).apply { attach() } },
        update = {
            set(createLayer) { this.createLayer = it }
            set(radius) { expr ->
                (layer as? MlCircleLayer)?.setProperties(
                    PropertyFactory.circleRadius(expr?.toMapLibre() ?: return@set)
                )
            }
            set(color) { expr ->
                (layer as? MlCircleLayer)?.setProperties(
                    PropertyFactory.circleColor(expr?.toMapLibre() ?: return@set)
                )
            }
            set(opacity) { expr ->
                (layer as? MlCircleLayer)?.setProperties(
                    PropertyFactory.circleOpacity(expr?.toMapLibre() ?: return@set)
                )
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
    val createLayer: () -> Layer = {
        MlFillLayer(id, sourceId).apply {
            filter?.let { setFilter(it.toMapLibre()) }
            val props = buildList {
                color?.let { add(PropertyFactory.fillColor(it.toMapLibre())) }
                opacity?.let { add(PropertyFactory.fillOpacity(it.toMapLibre())) }
            }
            if (props.isNotEmpty()) setProperties(*props.toTypedArray())
        }
    }
    ComposeNode<LayerNode, MapApplier>(
        factory = { LayerNode(mapApplier.style, id, createLayer).apply { attach() } },
        update = {
            set(createLayer) { this.createLayer = it }
            set(color) { expr ->
                (layer as? MlFillLayer)?.setProperties(
                    PropertyFactory.fillColor(expr?.toMapLibre() ?: return@set)
                )
            }
            set(opacity) { expr ->
                (layer as? MlFillLayer)?.setProperties(
                    PropertyFactory.fillOpacity(expr?.toMapLibre() ?: return@set)
                )
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
    val createLayer: () -> Layer = {
        MlLineLayer(id, sourceId).apply {
            sourceLayer?.let { this.sourceLayer = it }
            filter?.let { setFilter(it.toMapLibre()) }
            val props = buildList {
                color?.let { add(PropertyFactory.lineColor(it.toMapLibre())) }
                width?.let { add(PropertyFactory.lineWidth(it.toMapLibre())) }
                opacity?.let { add(PropertyFactory.lineOpacity(it.toMapLibre())) }
            }
            if (props.isNotEmpty()) setProperties(*props.toTypedArray())
        }
    }
    ComposeNode<LayerNode, MapApplier>(
        factory = { LayerNode(mapApplier.style, id, createLayer).apply { attach() } },
        update = { set(createLayer) { this.createLayer = it } },
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
    val createLayer: () -> Layer = {
        MlSymbolLayer(id, sourceId).apply {
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
    }
    ComposeNode<LayerNode, MapApplier>(
        factory = { LayerNode(mapApplier.style, id, createLayer).apply { attach() } },
        update = { set(createLayer) { this.createLayer = it } },
    )
}

@Composable
actual fun RasterStyleLayer(
    id: String,
    sourceId: String,
) {
    val mapApplier = LocalMapApplier.current
    val createLayer: () -> Layer = { MlRasterLayer(id, sourceId) }
    ComposeNode<LayerNode, MapApplier>(
        factory = { LayerNode(mapApplier.style, id, createLayer).apply { attach() } },
        update = { set(createLayer) { this.createLayer = it } },
    )
}

internal class LayerNode(
    val style: MutableState<Style?>,
    val layerId: String,
    var createLayer: () -> Layer,
) : MapNode {
    var layer: Layer? = null
        private set

    fun attach() {
        val s = style.value ?: return
        s.getLayer(layerId)?.let { existing -> s.removeLayer(existing) }
        layer = createLayer().also { s.addLayer(it) }
    }

    override fun onRemoved() {
        val s = style.value ?: return
        s.getLayer(layerId)?.let { runCatching { s.removeLayer(it) } }
        layer = null
    }

    override fun onCleared() = onRemoved()

    fun reattach() {
        val s = style.value ?: return
        // When the old style is destroyed during a style swap, the native peer
        // of any Layer previously added to it is invalidated; the Kotlin object
        // cannot be re-added (it crashes maplibre-native). Build a fresh Layer
        // for the new style instead, preserving any properties updated since.
        //
        // A rapid sequence of swaps can also deliver more than one "style
        // loaded" callback that resolves against the same style, so skip if the
        // layer is already present to avoid adding the same id twice.
        if (s.getLayer(layerId) != null) return
        try {
            layer = createLayer().also { s.addLayer(it) }
        } catch (_: IllegalStateException) {
            // Style is being replaced
        }
    }
}
