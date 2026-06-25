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
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource as MlGeoJsonSource
import org.maplibre.android.style.sources.RasterSource as MlRasterSource
import org.maplibre.android.style.sources.Source
import org.maplibre.android.style.sources.VectorSource as MlVectorSource
import java.net.URI

@Composable
actual fun GeoJsonSource(
    id: String,
    uri: String?,
    geoJson: String?,
    cluster: Boolean,
    clusterMaxZoom: Int,
    clusterRadius: Int,
) {
    val mapApplier = LocalMapApplier.current
    val createSource: () -> Source = {
        when {
            uri != null -> {
                val options = GeoJsonOptions()
                    .withCluster(cluster)
                    .withClusterMaxZoom(clusterMaxZoom)
                    .withClusterRadius(clusterRadius)
                MlGeoJsonSource(id, URI(uri), options)
            }
            geoJson != null -> {
                MlGeoJsonSource(id).apply { setGeoJson(geoJson) }
            }
            else -> MlGeoJsonSource(id)
        }
    }
    ComposeNode<SourceNode, MapApplier>(
        factory = { SourceNode(mapApplier.style, id, createSource).apply { attach() } },
        update = { set(createSource) { this.createSource = it } },
    )
}

@Composable
actual fun RasterSource(
    id: String,
    tileJsonUrl: String,
) {
    val mapApplier = LocalMapApplier.current
    val createSource: () -> Source = { MlRasterSource(id, tileJsonUrl) }
    ComposeNode<SourceNode, MapApplier>(
        factory = { SourceNode(mapApplier.style, id, createSource).apply { attach() } },
        update = { set(createSource) { this.createSource = it } },
    )
}

@Composable
actual fun VectorSource(
    id: String,
    tileJsonUrl: String,
) {
    val mapApplier = LocalMapApplier.current
    val createSource: () -> Source = { MlVectorSource(id, tileJsonUrl) }
    ComposeNode<SourceNode, MapApplier>(
        factory = { SourceNode(mapApplier.style, id, createSource).apply { attach() } },
        update = { set(createSource) { this.createSource = it } },
    )
}

internal class SourceNode(
    val style: MutableState<Style?>,
    val sourceId: String,
    var createSource: () -> Source,
) : MapNode {
    fun attach() {
        val s = style.value ?: return
        s.getSource(sourceId)?.let { existing -> s.removeSource(existing) }
        s.addSource(createSource())
    }

    override fun onRemoved() {
        val s = style.value ?: return
        s.getSource(sourceId)?.let { runCatching { s.removeSource(it) } }
    }

    override fun onCleared() = onRemoved()

    fun reattach() {
        val s = style.value ?: return
        // When the old style is destroyed during a style swap, the native peer
        // of any Source previously added to it is invalidated; the Kotlin
        // object cannot be re-added (it crashes maplibre-native). Build a fresh
        // Source for the new style instead.
        //
        // A rapid sequence of swaps can also deliver more than one "style
        // loaded" callback that resolves against the same style, so skip if the
        // source is already present to avoid adding the same id twice.
        if (s.getSource(sourceId) != null) return
        try {
            s.addSource(createSource())
        } catch (_: IllegalStateException) {
            // Style is being replaced
        }
    }
}
