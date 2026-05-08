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
import MapLibre.MLNSymbolStyleLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIColor

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

@OptIn(ExperimentalForeignApi::class)
class MapApplier(
    val mapView: MLNMapView,
) : BaseMapApplier() {

    /** Retained to prevent GC — UIKit gesture recognizers hold weak target references. */
    internal var dragHandler: AnnotationDragHandler? = null

    private val registeredLayerIds = mutableSetOf<String>()
    private val pendingOrders = mutableListOf<PendingLayerOrder>()
    private val committedOrders = mutableListOf<PendingLayerOrder>()
    private var declarationCounter = 0
    private val declarationOrder = mutableMapOf<String, Int>()

    val style: MLNStyle? get() = mapView.style

    fun addSourceAndLayer(
        source: MLNShapeSource,
        layer: MLNStyleLayer,
        aboveLayerId: String? = null,
        belowLayerId: String? = null,
    ) {
        style?.let {
            it.layerWithIdentifier(layer.identifier)?.let { existing ->
                it.removeLayer(existing)
            }
            it.sourceWithIdentifier(source.identifier)?.let { existing ->
                it.removeSource(existing)
            }
            it.addSource(source)
            it.addLayer(layer)
        }

        val layerId = layer.identifier
        registeredLayerIds.add(layerId)
        if (layerId !in declarationOrder) {
            declarationOrder[layerId] = declarationCounter++
        }

        if (aboveLayerId != null || belowLayerId != null) {
            pendingOrders.add(PendingLayerOrder(layerId, aboveLayerId, belowLayerId))
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun defaultFontNames(): List<String>? {
        val layers = style?.layers as? List<MLNStyleLayer> ?: return null
        for (layer in layers) {
            if (layer is MLNSymbolStyleLayer) {
                val fonts = layer.textFontNames?.expressionValueWithObject(null, context = null)
                if (fonts is List<*> && fonts.isNotEmpty()) {
                    return fonts as List<String>
                }
            }
        }
        return null
    }

    fun removeSourceAndLayer(sourceId: String, layerId: String) {
        style?.let { s ->
            s.layerWithIdentifier(layerId)?.let { s.removeLayer(it) }
            s.sourceWithIdentifier(sourceId)?.let { s.removeSource(it) }
        }
        registeredLayerIds.remove(layerId)
        declarationOrder.remove(layerId)
    }

    override fun onEndChanges() {
        super.onEndChanges()
        if (pendingOrders.isEmpty()) return

        committedOrders.addAll(pendingOrders)
        pendingOrders.clear()

        applyLayerOrdering()
    }

    private fun applyLayerOrdering() {
        val s = style ?: return
        if (committedOrders.isEmpty()) return

        val sortedOrder = computeLayerOrder(
            pendingOrders = committedOrders,
            registeredLayerIds = registeredLayerIds,
            declarationOrder = declarationOrder
        )

        var previousLayerId: String? = null
        for (layerId in sortedOrder) {
            val layer = s.layerWithIdentifier(layerId) ?: continue
            if (previousLayerId != null) {
                s.removeLayer(layer)
                val prev = s.layerWithIdentifier(previousLayerId) ?: continue
                s.insertLayer(layer, aboveLayer = prev)
            }
            previousLayerId = layerId
        }
    }

    override fun onClear() {
        super.onClear()
        registeredLayerIds.clear()
        pendingOrders.clear()
        committedOrders.clear()
        declarationOrder.clear()
        declarationCounter = 0
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
