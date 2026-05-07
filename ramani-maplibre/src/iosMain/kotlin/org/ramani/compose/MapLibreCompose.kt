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

import MapLibre.MLNMapView
import MapLibre.MLNPointFeature
import MapLibre.MLNShapeSource
import MapLibre.MLNStyleLayer
import MapLibre.MLNStyle
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIColor

interface MapNode {
    fun onAttached() {}
    fun onRemoved() {}
    fun onCleared() {}
}

internal interface DraggableNode {
    val isDraggable: Boolean
    val layerId: String
    val sourceId: String
    fun handleDrag(newCenter: LatLng)
    fun handleDragFinished(finalCenter: LatLng)
}

internal val LocalMapApplier = staticCompositionLocalOf<MapApplier> {
    error("MapApplier not provided. Map composables must be used inside a MapLibre { } block.")
}

private object MapNodeRoot : MapNode

@OptIn(ExperimentalForeignApi::class)
class MapApplier(
    val mapView: MLNMapView,
) : AbstractApplier<MapNode>(MapNodeRoot) {
    private val decorations = mutableListOf<MapNode>()

    /** Retained to prevent GC — UIKit gesture recognizers hold weak target references. */
    internal var dragHandler: AnnotationDragHandler? = null

    val style: MLNStyle? get() = mapView.style

    fun addSourceAndLayer(
        source: MLNShapeSource,
        layer: MLNStyleLayer,
        aboveLayerId: String? = null,
        belowLayerId: String? = null,
    ) {
        style?.let {
            it.addSource(source)
            val aboveLayer = aboveLayerId?.let { id -> it.layerWithIdentifier(id) }
            val belowLayer = belowLayerId?.let { id -> it.layerWithIdentifier(id) }
            when {
                aboveLayer != null -> it.insertLayer(layer, aboveLayer = aboveLayer)
                belowLayer != null -> it.insertLayer(layer, belowLayer = belowLayer)
                else -> it.addLayer(layer)
            }
        }
    }

    fun removeSourceAndLayer(sourceId: String, layerId: String) {
        style?.let { s ->
            s.layerWithIdentifier(layerId)?.let { s.removeLayer(it) }
            s.sourceWithIdentifier(sourceId)?.let { s.removeSource(it) }
        }
    }

    internal fun draggableLayerIds(): Set<String> {
        return decorations
            .filterIsInstance<DraggableNode>()
            .filter { it.isDraggable }
            .map { it.layerId }
            .toSet()
    }

    internal fun findDraggableNodeForLayerId(layerId: String): DraggableNode? {
        return decorations
            .filterIsInstance<DraggableNode>()
            .firstOrNull { it.isDraggable && it.layerId == layerId }
    }

    override fun insertBottomUp(index: Int, instance: MapNode) {
        // Ignored
    }

    override fun insertTopDown(index: Int, instance: MapNode) {
        decorations.add(index, instance)
        instance.onAttached()
    }

    override fun move(from: Int, to: Int, count: Int) {
    }

    override fun onClear() {
        decorations.forEach { it.onCleared() }
        decorations.clear()
    }

    override fun remove(index: Int, count: Int) {
        val toRemove = decorations.subList(index, index + count)
        toRemove.forEach { it.onRemoved() }
        toRemove.clear()
    }
}

internal fun newComposition(
    parent: CompositionContext,
    mapApplier: MapApplier,
    content: @Composable () -> Unit,
): Composition {
    return Composition(mapApplier, parent).apply {
        setContent(content)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class CircleNode(
    val mapApplier: MapApplier,
    override val sourceId: String,
    override val layerId: String,
    override var isDraggable: Boolean = false,
    var onCenterDragged: ((LatLng) -> Unit)? = null,
    var onDragFinished: ((LatLng) -> Unit)? = null,
    var centerState: CenterState? = null,
) : MapNode, DraggableNode {
    override fun onRemoved() {
        mapApplier.removeSourceAndLayer(sourceId, layerId)
    }

    override fun onCleared() {
        mapApplier.removeSourceAndLayer(sourceId, layerId)
    }

    override fun handleDrag(newCenter: LatLng) {
        val feature = MLNPointFeature()
        feature.setCoordinate(newCenter.toCLLocationCoordinate2D())
        (mapApplier.style?.sourceWithIdentifier(sourceId) as? MLNShapeSource)?.shape = feature
        centerState?.updateCenterFromDrag(newCenter)
        onCenterDragged?.invoke(newCenter)
    }

    override fun handleDragFinished(finalCenter: LatLng) {
        onDragFinished?.invoke(finalCenter)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class SymbolNode(
    val mapApplier: MapApplier,
    override val sourceId: String,
    override val layerId: String,
    override var isDraggable: Boolean = false,
    var onSymbolDragged: ((LatLng) -> Unit)? = null,
    var onDragFinished: ((LatLng) -> Unit)? = null,
    var centerState: CenterState? = null,
) : MapNode, DraggableNode {
    override fun onRemoved() {
        mapApplier.removeSourceAndLayer(sourceId, layerId)
    }

    override fun onCleared() {
        mapApplier.removeSourceAndLayer(sourceId, layerId)
    }

    override fun handleDrag(newCenter: LatLng) {
        val feature = MLNPointFeature()
        feature.setCoordinate(newCenter.toCLLocationCoordinate2D())
        (mapApplier.style?.sourceWithIdentifier(sourceId) as? MLNShapeSource)?.shape = feature
        centerState?.updateCenterFromDrag(newCenter)
        onSymbolDragged?.invoke(newCenter)
    }

    override fun handleDragFinished(finalCenter: LatLng) {
        onDragFinished?.invoke(finalCenter)
    }
}

internal class PolyLineNode(
    val mapApplier: MapApplier,
    val sourceId: String,
    val layerId: String,
) : MapNode {
    override fun onRemoved() {
        mapApplier.removeSourceAndLayer(sourceId, layerId)
    }

    override fun onCleared() {
        mapApplier.removeSourceAndLayer(sourceId, layerId)
    }
}

internal class FillNode(
    val mapApplier: MapApplier,
    val sourceId: String,
    val layerId: String,
) : MapNode {
    override fun onRemoved() {
        mapApplier.removeSourceAndLayer(sourceId, layerId)
    }

    override fun onCleared() {
        mapApplier.removeSourceAndLayer(sourceId, layerId)
    }
}

internal class MapObserverNode(
    var onMapMoved: () -> Unit,
    var onMapScaled: () -> Unit,
    var onMapRotated: (Double) -> Unit,
) : MapNode

@OptIn(ExperimentalForeignApi::class)
internal class MapPropertiesNode(
    val mapView: MLNMapView,
    val cameraPositionState: CameraPositionState,
) : MapNode

internal fun parseColor(color: String): UIColor {
    val trimmed = color.trim()

    if (trimmed.startsWith("#")) {
        val hex = trimmed.removePrefix("#")
        val value = hex.toLongOrNull(16) ?: return UIColor.clearColor
        return when (hex.length) {
            6 -> UIColor(
                red = ((value shr 16) and 0xFF) / 255.0,
                green = ((value shr 8) and 0xFF) / 255.0,
                blue = (value and 0xFF) / 255.0,
                alpha = 1.0,
            )
            8 -> UIColor(
                red = ((value shr 24) and 0xFF) / 255.0,
                green = ((value shr 16) and 0xFF) / 255.0,
                blue = ((value shr 8) and 0xFF) / 255.0,
                alpha = (value and 0xFF) / 255.0,
            )
            else -> UIColor.clearColor
        }
    }

    return when (trimmed.lowercase()) {
        "red" -> UIColor.redColor
        "green" -> UIColor.greenColor
        "blue" -> UIColor.blueColor
        "yellow" -> UIColor.yellowColor
        "white" -> UIColor.whiteColor
        "black" -> UIColor.blackColor
        "orange" -> UIColor.orangeColor
        "purple" -> UIColor.purpleColor
        "cyan" -> UIColor.cyanColor
        "magenta" -> UIColor.magentaColor
        "brown" -> UIColor.brownColor
        "gray", "grey" -> UIColor.grayColor
        "transparent", "clear" -> UIColor.clearColor
        else -> UIColor.clearColor
    }
}
