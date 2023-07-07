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
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.annotation.Annotation
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationManager
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.OnCircleAnnotationDragListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.gestures.addOnScaleListener
import kotlinx.coroutines.awaitCancellation
import kotlin.math.abs

internal suspend inline fun disposingComposition(factory: () -> Composition) {
    val composition = factory()
    try {
        awaitCancellation()
    } finally {
        composition.dispose()
    }
}

internal suspend fun MapView.newComposition(
    parent: CompositionContext,
    content: @Composable () -> Unit,
): Composition {
    val map = awaitMap()
    return Composition(
        MapApplier(map, this), parent
    ).apply {
        setContent(content)
    }
}

internal interface MapNode {
    fun onAttached() {}
    fun onRemoved() {}
    fun onCleared() {}
}

private object MapNodeRoot : MapNode

internal class MapApplier(
    val map: MapboxMap,
    val mapView: MapView,
) : AbstractApplier<MapNode>(MapNodeRoot) {
    private val decorations = mutableListOf<MapNode>()

    private val circleManagerMap = mutableMapOf<Int, CircleAnnotationManager>()
    private val lineManagerMap = mutableMapOf<Int, PolylineAnnotationManager>()
    private val polygonManagerMap = mutableMapOf<Int, PolygonAnnotationManager>()
    private val symbolManagerMap = mutableMapOf<Int, PointAnnotationManager>()

    private val zIndexReferenceAnnotationManagerMap =
        mutableMapOf<Int, Pair<AnnotationManager<*, *, *, *, *, *, *>, String>>()

    init {
        attachMapListeners()
    }

    private fun attachMapListeners() {
        map.addOnScaleListener(object : OnScaleListener {
            override fun onScaleBegin(detector: StandardScaleGestureDetector) {
                decorations
                    .filterIsInstance<MapObserverNode>()
                    .forEach { it.onMapScaled.invoke() }
            }

            override fun onScale(detector: StandardScaleGestureDetector) {
                decorations
                    .filterIsInstance<MapObserverNode>()
                    .forEach { it.onMapScaled.invoke() }
            }

            override fun onScaleEnd(detector: StandardScaleGestureDetector) {
            }
        })

        map.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                decorations.forEach {
                    if (it is MapObserverNode) {
                        it.onMapMoved.invoke()
                    }
                }
            }

            override fun onMove(detector: MoveGestureDetector): Boolean {
                decorations.forEach {
                    if (it is MapObserverNode) {
                        it.onMapMoved.invoke()
                    }
                }
                return false// TODO should sometimes return true? -> true means that it consumes the event
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {
            }
        })
    }

    fun getOrCreateCircleManagerForZIndex(zIndex: Int): CircleAnnotationManager {
        circleManagerMap[zIndex]?.let { return it }

        val annotationConfig = createAnnotationConfig(zIndex)
        val annotationApi = mapView.annotations
        val circleManager = annotationApi.createCircleAnnotationManager(annotationConfig)

        circleManagerMap[zIndex] = circleManager

        if (!zIndexReferenceAnnotationManagerMap.containsKey(zIndex)) {
            zIndexReferenceAnnotationManagerMap[zIndex] = Pair(circleManager, "$zIndex")
        }

        circleManager.addDragListener(object : OnCircleAnnotationDragListener {
            override fun onAnnotationDragStarted(annotation: Annotation<*>) {
                decorations.findInputCallback<CircleNode, CircleAnnotation, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation.id && it.circleManagerId == zIndex },
                    nodeInputCallback = { onCircleDragged }
                )?.let {
                    val circleAnnotation = annotation as CircleAnnotation
                    it(circleAnnotation)
                }
            }

            override fun onAnnotationDrag(annotation: Annotation<*>) {
                decorations.findInputCallback<CircleNode, CircleAnnotation, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation.id && it.circleManagerId == zIndex },
                    nodeInputCallback = { onCircleDragged }
                )?.let {
                    val circleAnnotation = annotation as CircleAnnotation
                    it(circleAnnotation)
                }
            }

            override fun onAnnotationDragFinished(annotation: Annotation<*>) {
                decorations.findInputCallback<CircleNode, CircleAnnotation, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation.id && it.circleManagerId == zIndex },
                    nodeInputCallback = { onCircleDragStopped }
                )?.let {
                    val circleAnnotation = annotation as CircleAnnotation
                    it(circleAnnotation)
                }
            }
        })

        return circleManagerMap[zIndex]!!
    }

    private fun createAnnotationConfig(zIndex: Int): AnnotationConfig {
        val layerId: String? = if (!zIndexReferenceAnnotationManagerMap.containsKey(zIndex)) {
            "$zIndex"
        } else {
            null
        }

        val layerInsertInfo = getLayerInsertInfoForZIndex(zIndex)

        val annotationConfig = when (layerInsertInfo.insertPosition) {
            LayerInsertMethod.NONE -> AnnotationConfig(layerId = layerId)
            LayerInsertMethod.INSERT_BELOW -> {
                AnnotationConfig(
                    layerId = layerId,
                    belowLayerId = layerInsertInfo.referenceLayerId
                )
            }

            else -> {
                // Should insert above, but apparently not supported by Mapbox
                AnnotationConfig(layerId = layerId)
            }
        }
        return annotationConfig
    }

    private fun getLayerInsertInfoForZIndex(zIndex: Int): LayerInsertInfo {
        val keys = zIndexReferenceAnnotationManagerMap.keys.sorted()

        if (keys.isEmpty()) {
            return LayerInsertInfo()
        }

        val closestLayerIndex = keys.map {
            abs(it - zIndex)
        }.withIndex().minBy { it.value }.index

        return LayerInsertInfo(
            zIndexReferenceAnnotationManagerMap[keys[closestLayerIndex]]?.second!!,
            if (zIndex > keys[closestLayerIndex]) LayerInsertMethod.INSERT_ABOVE else LayerInsertMethod.INSERT_BELOW
        )
    }

    fun getOrCreateSymbolManagerForZIndex(zIndex: Int): PointAnnotationManager {
        symbolManagerMap[zIndex]?.let { return it }

        val annotationConfig = createAnnotationConfig(zIndex)
        val annotationApi = mapView.annotations
        val symbolManager = annotationApi.createPointAnnotationManager(annotationConfig)

        symbolManagerMap[zIndex] = symbolManager

        if (!zIndexReferenceAnnotationManagerMap.containsKey(zIndex)) {
            zIndexReferenceAnnotationManagerMap[zIndex] = Pair(symbolManager, "$zIndex")
        }

        return symbolManager
    }

    fun getOrCreatePolygonManagerForZIndex(zIndex: Int): PolygonAnnotationManager {
        polygonManagerMap[zIndex]?.let { return it }

        val annotationConfig = createAnnotationConfig(zIndex)
        val annotationApi = mapView.annotations
        val polygonManager = annotationApi.createPolygonAnnotationManager(annotationConfig)

        polygonManagerMap[zIndex] = polygonManager

        if (!zIndexReferenceAnnotationManagerMap.containsKey(zIndex)) {
            zIndexReferenceAnnotationManagerMap[zIndex] = Pair(polygonManager, "$zIndex")
        }

        return polygonManager
    }

    fun getOrCreateLineManagerForZIndex(zIndex: Int): PolylineAnnotationManager {
        lineManagerMap[zIndex]?.let { return it }

        val annotationConfig = createAnnotationConfig(zIndex)
        val annotationApi = mapView.annotations
        val lineManager = annotationApi.createPolylineAnnotationManager(annotationConfig)

        lineManagerMap[zIndex] = lineManager

        if (!zIndexReferenceAnnotationManagerMap.containsKey(zIndex)) {
            zIndexReferenceAnnotationManagerMap[zIndex] = Pair(lineManager, "$zIndex")
        }

        return lineManager
    }

    override fun insertBottomUp(index: Int, instance: MapNode) {
        // TODO: implement properly
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

data class LayerInsertInfo(
    val referenceLayerId: String = "",
    val insertPosition: LayerInsertMethod = LayerInsertMethod.NONE,
)

enum class LayerInsertMethod {
    NONE,
    INSERT_BELOW,
    INSERT_ABOVE
}

internal class CircleNode(
    val circleManager: CircleAnnotationManager,
    val circleManagerId: Int,
    val circle: CircleAnnotation,
    var onCircleDragged: (CircleAnnotation) -> Unit,
    var onCircleDragStopped: (CircleAnnotation) -> Unit
) : MapNode {
    override fun onRemoved() {
        circleManager.delete(circle)
    }

    override fun onCleared() {
        circleManager.delete(circle)
    }
}

internal class SymbolNode(
    val symbolManager: PointAnnotationManager,
    val symbol: PointAnnotation,
) : MapNode {
    override fun onRemoved() {
        symbolManager.delete(symbol)
    }

    override fun onCleared() {
        symbolManager.delete(symbol)
    }
}

internal class PolylineNode(
    val lineManager: PolylineAnnotationManager,
    val polyLine: PolylineAnnotation,
) : MapNode {
    override fun onRemoved() {
        lineManager.delete(polyLine)
    }

    override fun onCleared() {
        lineManager.delete(polyLine)
    }
}

internal class PolygonNode(
    val polygonManager: PolygonAnnotationManager,
    val polygon: PolygonAnnotation,
) : MapNode {
    override fun onRemoved() {
        polygonManager.delete(polygon)
    }

    override fun onCleared() {
        polygonManager.delete(polygon)
    }
}

internal class MapObserverNode(
    var onMapMoved: () -> Unit,
    var onMapScaled: () -> Unit,
) : MapNode {
    override fun onRemoved() {
    }
}

private inline fun <reified NodeT : MapNode, I, O> Iterable<MapNode>.findInputCallback(
    nodeMatchPredicate: (NodeT) -> Boolean,
    nodeInputCallback: NodeT.() -> ((I) -> O)?,
): ((I) -> O)? {
    for (item in this) {
        if (item is NodeT && nodeMatchPredicate(item)) {
            // Found a matching node
            return nodeInputCallback(item)
        }
    }
    return null
}
