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
    ComposeNode<SourceNode, MapApplier>(
        factory = {
            val source = when {
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
            SourceNode(mapApplier.style, source).apply { attach() }
        },
        update = {},
    )
}

@Composable
actual fun RasterSource(
    id: String,
    tileJsonUrl: String,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<SourceNode, MapApplier>(
        factory = {
            SourceNode(mapApplier.style, MlRasterSource(id, tileJsonUrl)).apply { attach() }
        },
        update = {},
    )
}

@Composable
actual fun VectorSource(
    id: String,
    tileJsonUrl: String,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<SourceNode, MapApplier>(
        factory = {
            SourceNode(mapApplier.style, MlVectorSource(id, tileJsonUrl)).apply { attach() }
        },
        update = {},
    )
}

internal class SourceNode(
    val style: androidx.compose.runtime.MutableState<org.maplibre.android.maps.Style?>,
    val source: Source,
) : MapNode {
    fun attach() {
        style.value?.let { s ->
            s.getSource(source.id)?.let { existing -> s.removeSource(existing) }
            s.addSource(source)
        }
    }

    override fun onRemoved() {
        style.value?.removeSource(source)
    }

    override fun onCleared() {
        style.value?.removeSource(source)
    }

    fun reattach() {
        try {
            style.value?.addSource(source)
        } catch (_: IllegalStateException) {
            // Style is being replaced
        }
    }
}
