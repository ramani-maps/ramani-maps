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
import androidx.compose.runtime.staticCompositionLocalOf
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
import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.sources.Source
import org.maplibre.android.utils.BitmapUtils
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    suspendCancellableCoroutine { continuation ->
        setStyle(styleBuilder) { style ->
            continuation.resume(style)
        }
    }

interface MapNode {
    fun onAttached() {}
    fun onRemoved() {}
    fun onCleared() {}
}

internal data class PendingLayerOrder(
    val layerId: String,
    val aboveLayerId: String?,
    val belowLayerId: String?
)

internal fun computeLayerOrder(
    pendingOrders: List<PendingLayerOrder>,
    registeredLayerIds: Set<String>,
    declarationOrder: Map<String, Int>
): List<String> {
    // Collect every layer that participates in ordering (subjects + references).
    val involvedLayerIds = mutableSetOf<String>()
    for (order in pendingOrders) {
        involvedLayerIds.add(order.layerId)
        order.aboveLayerId?.let { involvedLayerIds.add(it) }
        order.belowLayerId?.let { involvedLayerIds.add(it) }
    }
    involvedLayerIds.retainAll(registeredLayerIds)

    // Build a DAG: edge A -> B means "A must be below B in the final stack".
    val adj = involvedLayerIds.associateWith { mutableListOf<String>() }
    val inDegree = involvedLayerIds.associateWithTo(mutableMapOf()) { 0 }

    for (order in pendingOrders) {
        val layerId = order.layerId
        val above = order.aboveLayerId
        val below = order.belowLayerId

        if (above != null && above in involvedLayerIds && layerId in involvedLayerIds) {
            adj[above]!!.add(layerId)
            inDegree[layerId] = inDegree[layerId]!! + 1
        }
        if (below != null && below in involvedLayerIds && layerId in involvedLayerIds) {
            adj[layerId]!!.add(below)
            inDegree[below] = inDegree[below]!! + 1
        }
    }

    // Kahn's algorithm with declaration-order tiebreaker: when several layers
    // have no remaining dependencies the one declared earliest goes first
    // (= lower in the layer stack), preserving compose-tree order for
    // independent layers.
    val queue = java.util.PriorityQueue<String>(compareBy { declarationOrder[it] ?: Int.MAX_VALUE })
    for ((id, deg) in inDegree) {
        if (deg == 0) queue.add(id)
    }

    val sortedOrder = mutableListOf<String>()
    while (queue.isNotEmpty()) {
        val current = queue.poll()
        current?.let {
            sortedOrder.add(current)
            for (neighbor in adj[current]!!) {
                val newDeg = inDegree[neighbor]!! - 1
                inDegree[neighbor] = newDeg
                if (newDeg == 0) queue.add(neighbor)
            }
        }
    }

    return sortedOrder
}

internal val LocalMapApplier = staticCompositionLocalOf<MapApplier> {
    error("MapApplier not provided. Map composables must be used inside a MapLibre { } block.")
}

private object MapNodeRoot : MapNode

class MapApplier(
    val map: MapLibreMap,
    val mapView: MapView,
    val style: MutableState<Style?>
) : AbstractApplier<MapNode>(MapNodeRoot) {
    private val decorations = mutableListOf<MapNode>()

    private val circleManagerByLayerId = mutableMapOf<String, CircleManager>()
    private val fillManagerByLayerId = mutableMapOf<String, FillManager>()
    private val symbolManagerByLayerId = mutableMapOf<String, SymbolManager>()
    private val lineManagerByLayerId = mutableMapOf<String, LineManager>()

    private val namedLayerRegistry = mutableMapOf<String, AnnotationManager<*, *, *, *, *, *>>()

    private val layerAliases = mutableMapOf<String, String>()
    private val pendingOrders = mutableListOf<PendingLayerOrder>()
    private val committedOrders = mutableListOf<PendingLayerOrder>()

    init {
        attachMapListeners()

        // Registered before any annotation managers, so this fires first on style change.
        // Sources and layers are re-added before annotation managers recreate their layers,
        // preserving declaration order (MapLayer below annotations).
        mapView.addOnDidFinishLoadingStyleListener {
            map.getStyle { newStyle ->
                style.value = newStyle
                reattachStyleNodes()
                // Annotation managers recreate their layers in their own
                // OnDidFinishLoadingStyleListener callbacks, which fire after
                // ours (registered later). Post to the next frame so all
                // managers have rebuilt before we reapply layer ordering.
                mapView.post { applyLayerOrdering(newStyle) }
            }
        }
    }

    fun reattachStyleNodes() {
        decorations.forEach { node ->
            when (node) {
                is SourceNode -> node.reattach()
                is LayerNode -> node.reattach()
                is ImageNode -> node.reattach()
            }
        }
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

    private fun <M : AnnotationManager<*, *, *, *, *, *>> getOrCreateManager(
        layerId: String,
        aboveLayerId: String?,
        belowLayerId: String?,
        cache: MutableMap<String, M>,
        factory: (MapView, MapLibreMap, Style) -> M,
        attachListeners: ((M) -> Unit)? = null
    ): M {
        cache[layerId]?.let { return it }

        val style = checkNotNull(style.value)

        if (aboveLayerId != null || belowLayerId != null) {
            pendingOrders.add(PendingLayerOrder(layerId, aboveLayerId, belowLayerId))
        }
        val manager = factory(mapView, map, style)

        cache[layerId] = manager
        namedLayerRegistry[layerId] = manager

        attachListeners?.invoke(manager)

        return manager
    }

    fun registerLayerAlias(alias: String, targetLayerId: String) {
        layerAliases[alias] = targetLayerId
    }

    fun getOrCreateCircleManagerForLayerId(
        layerId: String,
        aboveLayerId: String?,
        belowLayerId: String?
    ): CircleManager = getOrCreateManager(
        layerId, aboveLayerId, belowLayerId,
        circleManagerByLayerId,
        ::CircleManager,
        ::attachCircleManagerListeners
    )

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

    fun getOrCreateSymbolManagerForLayerId(
        layerId: String,
        aboveLayerId: String?,
        belowLayerId: String?
    ): SymbolManager = getOrCreateManager(
        layerId, aboveLayerId, belowLayerId,
        symbolManagerByLayerId,
        { mapView, map, style ->
            SymbolManager(mapView, map, style).apply { iconAllowOverlap = true }
        },
        ::attachSymbolManagerListeners
    )

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

    fun getOrCreateFillManagerForLayerId(
        layerId: String,
        aboveLayerId: String?,
        belowLayerId: String?
    ): FillManager = getOrCreateManager(
        layerId, aboveLayerId, belowLayerId,
        fillManagerByLayerId,
        ::FillManager
    )

    fun getOrCreateLineManagerForLayerId(
        layerId: String,
        aboveLayerId: String?,
        belowLayerId: String?
    ): LineManager = getOrCreateManager(
        layerId, aboveLayerId, belowLayerId,
        lineManagerByLayerId,
        ::LineManager
    )

    override fun onEndChanges() {
        super.onEndChanges()
        if (pendingOrders.isEmpty()) return

        committedOrders.addAll(pendingOrders)
        pendingOrders.clear()

        val style = style.value ?: return
        applyLayerOrdering(style)
    }

    private fun applyLayerOrdering(style: Style) {
        if (committedOrders.isEmpty()) return

        val resolvedOrders = committedOrders.map { order ->
            PendingLayerOrder(
                layerId = order.layerId,
                aboveLayerId = order.aboveLayerId?.let { layerAliases[it] ?: it },
                belowLayerId = order.belowLayerId?.let { layerAliases[it] ?: it }
            )
        }

        val declarationOrder = namedLayerRegistry.keys.withIndex()
            .associate { (index, key) -> key to index }

        val sortedOrder = computeLayerOrder(
            pendingOrders = resolvedOrders,
            registeredLayerIds = namedLayerRegistry.keys,
            declarationOrder = declarationOrder
        )

        // Walk the computed order and move each layer just above the previous
        // one, producing the desired total ordering.
        var previousInternalLayerId: String? = null
        for (layerId in sortedOrder) {
            val manager = namedLayerRegistry[layerId] ?: continue
            val internalLayerId = manager.layerId

            if (previousInternalLayerId != null) {
                val layer = style.getLayer(internalLayerId) ?: continue
                style.removeLayer(layer)
                style.addLayerAbove(layer, previousInternalLayerId)
            }

            previousInternalLayerId = internalLayerId
        }
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
        layerAliases.clear()
        committedOrders.clear()
    }

    override fun remove(index: Int, count: Int) {
        val toRemove = decorations.subList(index, index + count)
        toRemove.forEach { it.onRemoved() }
        toRemove.clear()
    }
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

internal class SourceNode(
    val style: MutableState<Style?>,
    val factory: () -> Source,
) : MapNode {
    var source: Source? = null
        private set
    var onUpdate: ((Source) -> Unit)? = null

    fun attach() {
        source = factory()
        style.value?.let { s ->
            s.getSource(source!!.id)?.let { existing -> s.removeSource(existing) }
            s.addSource(source!!)
        }
    }

    override fun onAttached() {
        // Source is added in attach(), called from the ComposeNode factory,
        // to run during composition and preserve declaration order.
    }

    override fun onRemoved() {
        source?.let { style.value?.removeSource(it) }
        source = null
    }

    override fun onCleared() {
        source?.let { style.value?.removeSource(it) }
        source = null
    }

    fun reattach() {
        try {
            source = factory()
            style.value?.addSource(source!!)
        } catch (_: IllegalStateException) {
            // Style is being replaced and will be re-added after the new style loads
        }
    }
}

internal class LayerNode(
    val style: MutableState<Style?>,
    val factory: () -> Layer,
) : MapNode {
    var layer: Layer? = null
        private set
    var onUpdate: ((Layer) -> Unit)? = null

    fun attach() {
        layer = factory()
        style.value?.let { s ->
            s.getLayer(layer!!.id)?.let { existing -> s.removeLayer(existing) }
            s.addLayer(layer!!)
        }
    }

    override fun onAttached() {
        // Layer is added in attach(), called from the ComposeNode factory,
        // to run during composition and preserve declaration order.
    }

    override fun onRemoved() {
        layer?.let { style.value?.removeLayer(it) }
        layer = null
    }

    override fun onCleared() {
        layer?.let { style.value?.removeLayer(it) }
        layer = null
    }

    fun reattach() {
        try {
            layer = factory()
            style.value?.addLayer(layer!!)
        } catch (_: IllegalStateException) {
            // Style is being replaced and will be re-added after the new style loads
        }
    }
}

internal class ImageNode(
    val style: MutableState<Style?>,
    val context: Context,
    val id: String,
    var drawableRes: Int,
) : MapNode {
    override fun onAttached() { loadImage() }

    override fun onRemoved() { style.value?.removeImage(id) }
    override fun onCleared() { style.value?.removeImage(id) }

    fun reattach() { loadImage() }

    fun loadImage() {
        try {
            val drawable = context.getDrawable(drawableRes)
            val bitmap = BitmapUtils.getBitmapFromDrawable(drawable)
            bitmap?.let { style.value?.addImage(id, it) }
        } catch (_: IllegalStateException) {
            // Style is being replaced and will be re-added after the new style loads
        }
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
