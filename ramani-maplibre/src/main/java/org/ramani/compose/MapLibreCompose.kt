/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2023 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.awaitCancellation
import org.maplibre.android.gestures.MoveGestureDetector
import org.maplibre.android.gestures.RotateGestureDetector
import org.maplibre.android.gestures.StandardScaleGestureDetector
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.OnMoveListener
import org.maplibre.android.maps.MapLibreMap.OnRotateListener
import org.maplibre.android.maps.MapLibreMap.OnScaleListener
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.AnnotationManager
import org.maplibre.android.plugins.annotation.Circle
import org.maplibre.android.plugins.annotation.CircleManager
import org.maplibre.android.plugins.annotation.Fill
import org.maplibre.android.plugins.annotation.FillManager
import org.maplibre.android.plugins.annotation.Line
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.OnCircleDragListener
import org.maplibre.android.plugins.annotation.OnSymbolDragListener
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

internal suspend inline fun disposingComposition(factory: () -> Composition) {
    val composition = factory()
    try {
        awaitCancellation()
    } finally {
        composition.dispose()
    }
}

internal fun MapView.newComposition(
    parent: CompositionContext,
    map: MapLibreMap,
    style: MutableState<Style?>,
    content: @Composable () -> Unit,
): Composition {
    return Composition(
        MapApplier(map, this, style), parent
    ).apply {
        setContent(content)
    }
}

internal suspend fun MapLibreMap.awaitStyle(styleBuilder: Style.Builder) =
    suspendCoroutine { continuation ->
        setStyle(styleBuilder) { style ->
            continuation.resume(style)
        }
    }

interface MapNode {
    fun onAttached() {}
    fun onRemoved() {}
    fun onCleared() {}
}

private object MapNodeRoot : MapNode

class MapApplier(
    val map: MapLibreMap,
    val mapView: MapView,
    val style: MutableState<Style?>
) : AbstractApplier<MapNode>(MapNodeRoot) {
    private val decorations = mutableListOf<MapNode>()

    private val circleManagerMap = mutableMapOf<Int, CircleManager>()
    private val fillManagerMap = mutableMapOf<Int, FillManager>()
    private val symbolManagerMap = mutableMapOf<Int, SymbolManager>()
    private val lineManagerMap = mutableMapOf<Int, LineManager>()

    private val zIndexReferenceAnnotationManagerMap =
        mutableMapOf<Int, AnnotationManager<*, *, *, *, *, *>>()

    private val circleManagerByLayerId = mutableMapOf<String, CircleManager>()
    private val fillManagerByLayerId = mutableMapOf<String, FillManager>()
    private val symbolManagerByLayerId = mutableMapOf<String, SymbolManager>()
    private val lineManagerByLayerId = mutableMapOf<String, LineManager>()

    private val namedLayerRegistry = mutableMapOf<String, AnnotationManager<*, *, *, *, *, *>>()

    private data class PendingLayerOrder(
        val layerId: String,
        val aboveLayerId: String?,
        val belowLayerId: String?
    )

    private val pendingOrders = mutableListOf<PendingLayerOrder>()

    init {
        attachMapListeners()
    }

    private fun attachMapListeners() {
        map.addOnCameraMoveListener {
            decorations
                .filterIsInstance<MapObserverNode>()
                .forEach {
                    it.onMapMoved()
                    it.onMapScaled()
                    it.onMapRotated.invoke(map.cameraPosition.bearing)
                }
        }

        map.addOnScaleListener(object : OnScaleListener {
            override fun onScaleBegin(detector: StandardScaleGestureDetector) {
                decorations
                    .filterIsInstance<MapObserverNode>()
                    .forEach { it.onMapScaled() }
            }

            override fun onScale(detector: StandardScaleGestureDetector) {
                decorations
                    .filterIsInstance<MapObserverNode>()
                    .forEach { it.onMapScaled() }
            }

            override fun onScaleEnd(detector: StandardScaleGestureDetector) {
            }
        })

        map.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                decorations
                    .filterIsInstance<MapObserverNode>()
                    .forEach { it.onMapMoved() }
            }

            override fun onMove(detector: MoveGestureDetector) {
                decorations
                    .filterIsInstance<MapObserverNode>()
                    .forEach { it.onMapMoved() }
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {
            }
        })

        map.addOnRotateListener(object : OnRotateListener {
            override fun onRotateBegin(detector: RotateGestureDetector) {
                decorations
                    .filterIsInstance<MapObserverNode>()
                    .forEach { it.onMapRotated(map.cameraPosition.bearing) }
            }

            override fun onRotate(detector: RotateGestureDetector) {
                decorations
                    .filterIsInstance<MapObserverNode>()
                    .forEach { it.onMapRotated(map.cameraPosition.bearing) }
            }

            override fun onRotateEnd(detector: RotateGestureDetector) {
                decorations
                    .filterIsInstance<MapObserverNode>()
                    .forEach { it.onMapRotated(map.cameraPosition.bearing) }
            }
        })
    }

    fun getOrCreateCircleManagerForZIndex(zIndex: Int): CircleManager {
        circleManagerMap[zIndex]?.let { return it }

        val style = checkNotNull(style.value)
        val layerInsertInfo = getLayerInsertInfoForZIndex(zIndex)

        val circleManager = layerInsertInfo?.let {
            CircleManager(
                mapView,
                map,
                style,
                if (it.insertPosition == LayerInsertMethod.INSERT_BELOW) it.referenceLayerId else null,
                if (it.insertPosition == LayerInsertMethod.INSERT_ABOVE) it.referenceLayerId else null
            )
        } ?: run {
            CircleManager(mapView, map, style)
        }

        circleManagerMap[zIndex] = circleManager

        if (!zIndexReferenceAnnotationManagerMap.containsKey(zIndex)) {
            zIndexReferenceAnnotationManagerMap[zIndex] = circleManager
        }

        attachCircleManagerListeners(circleManager)

        return circleManager
    }

    fun getOrCreateCircleManagerForLayerId(
        layerId: String,
        aboveLayerId: String?,
        belowLayerId: String?
    ): CircleManager {
        circleManagerByLayerId[layerId]?.let { return it }

        val style = checkNotNull(style.value)

        if (aboveLayerId != null || belowLayerId != null) {
            pendingOrders.add(PendingLayerOrder(layerId, aboveLayerId, belowLayerId))
        }
        val circleManager = CircleManager(mapView, map, style)

        circleManagerByLayerId[layerId] = circleManager
        namedLayerRegistry[layerId] = circleManager

        attachCircleManagerListeners(circleManager)

        return circleManager
    }

    private fun attachCircleManagerListeners(circleManager: CircleManager) {
        circleManager.addDragListener(object : OnCircleDragListener {
            override fun onAnnotationDragStarted(annotation: Circle) {
                decorations.findInputCallback<CircleNode, Circle, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation.id && it.circleManager.layerId == circleManager.layerId },
                    nodeInputCallback = { onCircleDragged }
                )?.invoke(annotation)
            }

            override fun onAnnotationDrag(annotation: Circle) {
                decorations.findInputCallback<CircleNode, Circle, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation.id && it.circleManager.layerId == circleManager.layerId },
                    nodeInputCallback = { onCircleDragged }
                )?.invoke(annotation)
            }

            override fun onAnnotationDragFinished(annotation: Circle) {
                decorations.findInputCallback<CircleNode, Circle, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation.id && it.circleManager.layerId == circleManager.layerId },
                    nodeInputCallback = { onCircleDragStopped }
                )?.invoke(annotation)
            }
        })

        circleManager.addClickListener { annotation ->
            decorations.findInputCallback<CircleNode, Circle, Unit>(
                nodeMatchPredicate = { it.circle.id == annotation.id && it.circleManager.layerId == circleManager.layerId },
                nodeInputCallback = { onCircleClicked }
            )?.invoke(annotation)
            true
        }

        circleManager.addLongClickListener { annotation ->
            decorations.findInputCallback<CircleNode, Circle, Unit>(
                nodeMatchPredicate = { it.circle.id == annotation.id && it.circleManager.layerId == circleManager.layerId },
                nodeInputCallback = { onCircleLongClicked }
            )?.invoke(annotation)
            true
        }
    }

    private fun getLayerInsertInfoForZIndex(zIndex: Int): LayerInsertInfo? {
        val keys = zIndexReferenceAnnotationManagerMap.keys.sorted()

        if (keys.isEmpty()) {
            return null
        }

        val closestLayerIndex = keys.map {
            abs(it - zIndex)
        }.withIndex().minBy { it.value }.index

        return LayerInsertInfo(
            checkNotNull(zIndexReferenceAnnotationManagerMap[keys[closestLayerIndex]]?.layerId),
            if (zIndex > keys[closestLayerIndex]) LayerInsertMethod.INSERT_ABOVE else LayerInsertMethod.INSERT_BELOW
        )
    }

    fun getOrCreateSymbolManagerForZIndex(zIndex: Int): SymbolManager {
        symbolManagerMap[zIndex]?.let { return it }

        val style = checkNotNull(style.value)
        val layerInsertInfo = getLayerInsertInfoForZIndex(zIndex)

        val symbolManager = layerInsertInfo?.let {
            SymbolManager(
                mapView,
                map,
                style,
                if (it.insertPosition == LayerInsertMethod.INSERT_BELOW) it.referenceLayerId else null,
                if (it.insertPosition == LayerInsertMethod.INSERT_ABOVE) it.referenceLayerId else null,
                null
            )
        } ?: run {
            SymbolManager(mapView, map, style)
        }

        symbolManager.iconAllowOverlap = true

        symbolManagerMap[zIndex] = symbolManager

        if (!zIndexReferenceAnnotationManagerMap.containsKey(zIndex)) {
            zIndexReferenceAnnotationManagerMap[zIndex] = symbolManager
        }

        attachSymbolManagerListeners(symbolManager)

        return symbolManager
    }

    fun getOrCreateSymbolManagerForLayerId(
        layerId: String,
        aboveLayerId: String?,
        belowLayerId: String?
    ): SymbolManager {
        symbolManagerByLayerId[layerId]?.let { return it }

        val style = checkNotNull(style.value)

        if (aboveLayerId != null || belowLayerId != null) {
            pendingOrders.add(PendingLayerOrder(layerId, aboveLayerId, belowLayerId))
        }
        val symbolManager = SymbolManager(mapView, map, style)

        symbolManager.iconAllowOverlap = true

        symbolManagerByLayerId[layerId] = symbolManager
        namedLayerRegistry[layerId] = symbolManager

        attachSymbolManagerListeners(symbolManager)

        return symbolManager
    }

    private fun attachSymbolManagerListeners(symbolManager: SymbolManager) {
        symbolManager.addDragListener(object : OnSymbolDragListener {
            override fun onAnnotationDragStarted(annotation: Symbol) {
                decorations.findInputCallback<SymbolNode, Symbol, Unit>(
                    nodeMatchPredicate = { it.symbol.id == annotation.id && it.symbolManager.layerId == symbolManager.layerId },
                    nodeInputCallback = { onSymbolDragged }
                )?.invoke(annotation)
            }

            override fun onAnnotationDrag(annotation: Symbol) {
                decorations.findInputCallback<SymbolNode, Symbol, Unit>(
                    nodeMatchPredicate = { it.symbol.id == annotation.id && it.symbolManager.layerId == symbolManager.layerId },
                    nodeInputCallback = { onSymbolDragged }
                )?.invoke(annotation)
            }

            override fun onAnnotationDragFinished(annotation: Symbol) {
                decorations.findInputCallback<SymbolNode, Symbol, Unit>(
                    nodeMatchPredicate = { it.symbol.id == annotation.id && it.symbolManager.layerId == symbolManager.layerId },
                    nodeInputCallback = { onSymbolDragStopped }
                )?.invoke(annotation)
            }
        })

        symbolManager.addClickListener { annotation ->
            decorations.findInputCallback<SymbolNode, Symbol, Unit>(
                nodeMatchPredicate = { it.symbol.id == annotation.id && it.symbolManager.layerId == symbolManager.layerId },
                nodeInputCallback = { onSymbolClicked }
            )?.invoke(annotation)
            true
        }

        symbolManager.addLongClickListener { annotation ->
            decorations.findInputCallback<SymbolNode, Symbol, Unit>(
                nodeMatchPredicate = { it.symbol.id == annotation.id && it.symbolManager.layerId == symbolManager.layerId },
                nodeInputCallback = { onSymbolLongClicked }
            )?.invoke(annotation)
            true
        }
    }

    fun getOrCreateFillManagerForZIndex(zIndex: Int): FillManager {
        fillManagerMap[zIndex]?.let { return it }

        val style = checkNotNull(style.value)
        val layerInsertInfo = getLayerInsertInfoForZIndex(zIndex)

        val fillManager = layerInsertInfo?.let {
            FillManager(
                mapView,
                map,
                style,
                if (it.insertPosition == LayerInsertMethod.INSERT_BELOW) it.referenceLayerId else null,
                if (it.insertPosition == LayerInsertMethod.INSERT_ABOVE) it.referenceLayerId else null,
                null
            )
        } ?: run {
            FillManager(mapView, map, style)
        }

        if (!zIndexReferenceAnnotationManagerMap.containsKey(zIndex)) {
            zIndexReferenceAnnotationManagerMap[zIndex] = fillManager
        }

        fillManagerMap[zIndex] = fillManager
        return fillManager
    }

    fun getOrCreateFillManagerForLayerId(
        layerId: String,
        aboveLayerId: String?,
        belowLayerId: String?
    ): FillManager {
        fillManagerByLayerId[layerId]?.let { return it }

        val style = checkNotNull(style.value)

        if (aboveLayerId != null || belowLayerId != null) {
            pendingOrders.add(PendingLayerOrder(layerId, aboveLayerId, belowLayerId))
        }
        val fillManager = FillManager(mapView, map, style)

        fillManagerByLayerId[layerId] = fillManager
        namedLayerRegistry[layerId] = fillManager

        return fillManager
    }

    fun getOrCreateLineManagerForZIndex(zIndex: Int): LineManager {
        lineManagerMap[zIndex]?.let { return it }

        val style = checkNotNull(style.value)
        val layerInsertInfo = getLayerInsertInfoForZIndex(zIndex)

        val lineManager = layerInsertInfo?.let {
            LineManager(
                mapView,
                map,
                style,
                if (it.insertPosition == LayerInsertMethod.INSERT_BELOW) it.referenceLayerId else null,
                if (it.insertPosition == LayerInsertMethod.INSERT_ABOVE) it.referenceLayerId else null,
                null
            )
        } ?: run {
            LineManager(mapView, map, style)
        }


        if (!zIndexReferenceAnnotationManagerMap.containsKey(zIndex)) {
            zIndexReferenceAnnotationManagerMap[zIndex] = lineManager
        }

        lineManagerMap[zIndex] = lineManager
        return lineManager
    }

    fun getOrCreateLineManagerForLayerId(
        layerId: String,
        aboveLayerId: String?,
        belowLayerId: String?
    ): LineManager {
        lineManagerByLayerId[layerId]?.let { return it }

        val style = checkNotNull(style.value)

        if (aboveLayerId != null || belowLayerId != null) {
            pendingOrders.add(PendingLayerOrder(layerId, aboveLayerId, belowLayerId))
        }
        val lineManager = LineManager(mapView, map, style)

        lineManagerByLayerId[layerId] = lineManager
        namedLayerRegistry[layerId] = lineManager

        return lineManager
    }

    override fun onEndChanges() {
        super.onEndChanges()
        if (pendingOrders.isEmpty()) return

        val style = style.value ?: return

        // Topological sort: process dependencies before dependents so that moving
        // layer B above A before moving C above B leaves C in the right place.
        val sorted = topologicalSort(pendingOrders)

        // When multiple layers share the same belowLayerId, each addLayerBelow
        // inserts just below the reference, pushing earlier siblings further down and
        // inverting declaration order.  Track the deepest sibling so far and insert
        // below *it* instead, keeping declaration order intact.
        val lastPlacedBelow = mutableMapOf<String, String>()

        for (order in sorted) {
            val manager = namedLayerRegistry[order.layerId] ?: continue
            val managerLayerId = manager.layerId

            val targetManager = when {
                order.aboveLayerId != null -> namedLayerRegistry[order.aboveLayerId]
                order.belowLayerId != null -> namedLayerRegistry[order.belowLayerId]
                else -> null
            } ?: continue

            val layer = style.getLayer(managerLayerId) ?: continue
            style.removeLayer(layer)

            if (order.aboveLayerId != null) {
                style.addLayerAbove(layer, targetManager.layerId)
            } else {
                val effectiveTarget = lastPlacedBelow[order.belowLayerId] ?: targetManager.layerId
                style.addLayerBelow(layer, effectiveTarget)
                lastPlacedBelow[order.belowLayerId!!] = managerLayerId
            }
        }
        pendingOrders.clear()
    }

    private fun topologicalSort(orders: List<PendingLayerOrder>): List<PendingLayerOrder> {
        val orderByLayerId = orders.associateBy { it.layerId }
        val result = mutableListOf<PendingLayerOrder>()
        val visited = mutableSetOf<String>()

        fun visit(order: PendingLayerOrder) {
            if (order.layerId in visited) return
            visited.add(order.layerId)
            val depLayerId = order.aboveLayerId ?: order.belowLayerId
            if (depLayerId != null) {
                orderByLayerId[depLayerId]?.let { visit(it) }
            }
            result.add(order)
        }

        for (order in orders.reversed()) visit(order)
        return result
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

data class LayerInsertInfo(val referenceLayerId: String, val insertPosition: LayerInsertMethod)

enum class LayerInsertMethod {
    INSERT_BELOW,
    INSERT_ABOVE
}

internal class CircleNode(
    val circleManager: CircleManager,
    val circle: Circle,
    var onCircleDragged: (Circle) -> Unit,
    var onCircleDragStopped: (Circle) -> Unit,
    var onCircleClicked: (Circle) -> Unit,
    var onCircleLongClicked: (Circle) -> Unit
) : MapNode {
    override fun onRemoved() {
        circleManager.delete(circle)
    }

    override fun onCleared() {
        circleManager.delete(circle)
    }
}

internal class SymbolNode(
    val symbolManager: SymbolManager,
    val symbol: Symbol,
    var onSymbolDragged: (Symbol) -> Unit,
    var onSymbolDragStopped: (Symbol) -> Unit,
    var onSymbolClicked: (Symbol) -> Unit,
    var onSymbolLongClicked: (Symbol) -> Unit
) : MapNode {
    override fun onRemoved() {
        symbolManager.delete(symbol)
    }

    override fun onCleared() {
        symbolManager.delete(symbol)
    }
}

internal class PolyLineNode(
    val lineManager: LineManager,
    val polyLine: Line,
) : MapNode {
    override fun onRemoved() {
        lineManager.delete(polyLine)
    }

    override fun onCleared() {
        lineManager.delete(polyLine)
    }
}

internal class FillNode(
    val fillManager: FillManager,
    val fill: Fill,
) : MapNode {
    override fun onRemoved() {
        fillManager.delete(fill)
    }

    override fun onCleared() {
        fillManager.delete(fill)
    }
}

internal class MapObserverNode(
    var onMapMoved: () -> Unit,
    var onMapScaled: () -> Unit,
    var onMapRotated: (Double) -> Unit,
) : MapNode {
    override fun onRemoved() {
    }
}

private inline fun <reified NodeT : MapNode, I, O> Iterable<MapNode>.findInputCallback(
    nodeMatchPredicate: (NodeT) -> Boolean,
    nodeInputCallback: NodeT.() -> ((I) -> O)?,
): ((I) -> O)? {
    val callback: ((I) -> O)? = null
    for (item in this) {
        if (item is NodeT && nodeMatchPredicate(item)) {
            // Found a matching node
            return nodeInputCallback(item)
        }
    }
    return callback
}
