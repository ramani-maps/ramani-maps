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

import MapLibre.MLNShapeSource
import MapLibre.MLNShapeSourceOptionClustered
import MapLibre.MLNShapeSourceOptionClusterRadius
import MapLibre.MLNShapeSourceOptionMaximumZoomLevelForClustering
import MapLibre.MLNRasterTileSource
import MapLibre.MLNVectorTileSource
import MapLibre.MLNShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import kotlinx.cinterop.ExperimentalForeignApi
import org.ramani.compose.LocalMapApplier
import org.ramani.compose.MapApplier
import org.ramani.compose.MapNode
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding

@OptIn(ExperimentalForeignApi::class)
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
            val options = mutableMapOf<Any?, Any?>()
            if (cluster) {
                options[MLNShapeSourceOptionClustered] = NSNumber(bool = true)
                options[MLNShapeSourceOptionMaximumZoomLevelForClustering] = NSNumber(int = clusterMaxZoom)
                options[MLNShapeSourceOptionClusterRadius] = NSNumber(int = clusterRadius)
            }

            val source = when {
                uri != null -> MLNShapeSource(
                    identifier = id,
                    URL = NSURL(string = uri),
                    options = options,
                )
                geoJson != null -> {
                    @Suppress("CAST_NEVER_SUCCEEDS")
                    val nsString = geoJson as NSString
                    val data = nsString.dataUsingEncoding(NSUTF8StringEncoding)!!
                    val shape = MLNShape.shapeWithData(
                        data = data,
                        encoding = NSUTF8StringEncoding,
                        error = null,
                    )
                    MLNShapeSource(identifier = id, shape = shape, options = options)
                }
                else -> MLNShapeSource(identifier = id, shape = null, options = options)
            }

            SourceNode(mapApplier, id, source).apply { attach() }
        },
        update = {},
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RasterSource(
    id: String,
    tileJsonUrl: String,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<SourceNode, MapApplier>(
        factory = {
            val source = MLNRasterTileSource(
                identifier = id,
                configurationURL = NSURL(string = tileJsonUrl),
            )
            SourceNode(mapApplier, id, source).apply { attach() }
        },
        update = {},
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VectorSource(
    id: String,
    tileJsonUrl: String,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<SourceNode, MapApplier>(
        factory = {
            val source = MLNVectorTileSource(
                identifier = id,
                configurationURL = NSURL(string = tileJsonUrl),
            )
            SourceNode(mapApplier, id, source).apply { attach() }
        },
        update = {},
    )
}

@OptIn(ExperimentalForeignApi::class)
internal class SourceNode(
    val mapApplier: MapApplier,
    val sourceId: String,
    val source: MapLibre.MLNSource,
) : MapNode {
    fun attach() {
        mapApplier.style?.let { s ->
            s.sourceWithIdentifier(sourceId)?.let { s.removeSource(it) }
            s.addSource(source)
        }
    }

    fun reattach() {
        attach()
    }

    override fun onRemoved() {
        mapApplier.style?.let { s ->
            s.sourceWithIdentifier(sourceId)?.let { s.removeSource(it) }
        }
    }

    override fun onCleared() {
        onRemoved()
    }
}

