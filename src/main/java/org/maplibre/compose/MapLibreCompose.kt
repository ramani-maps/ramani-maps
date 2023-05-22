package org.maplibre.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Circle
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager
import com.mapbox.mapboxsdk.plugins.annotation.Fill
import com.mapbox.mapboxsdk.plugins.annotation.FillManager
import com.mapbox.mapboxsdk.plugins.annotation.Line
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.OnCircleDragListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import kotlinx.coroutines.awaitCancellation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    style: Style,
    content: @Composable () -> Unit,
): Composition {
    val map = awaitMap()
    return Composition(
        MapApplier(map, this, style), parent
    ).apply {
        setContent(content)
    }
}

internal suspend fun MapboxMap.awaitStyle() = suspendCoroutine { continuation ->
    val key = "2z0TwvuXjwgOpvle5GYY"
    Helper.validateKey(key)
    val styleUrl = "https://api.maptiler.com/maps/satellite/style.json?key=${key}";
    setStyle(styleUrl) { style ->
        continuation.resume(style)
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
    mapView: MapView,
    val style: Style
) : AbstractApplier<MapNode>(MapNodeRoot) {
    private val decorations = mutableListOf<MapNode>()

    val topCircleManager: CircleManager = CircleManager(mapView, map, style)
    val circleManager: CircleManager = CircleManager(mapView, map, style, topCircleManager.layerId)

    val lineManager = LineManager(mapView, map, style, circleManager.layerId)
    val fillManager = FillManager(mapView, map, style, circleManager.layerId)
    val symbolManager = SymbolManager(mapView, map, style, circleManager.layerId)

    init {
        attachCircleDragListeners()
    }

    private fun attachCircleDragListeners() {
        circleManager.addDragListener(object : OnCircleDragListener {
            override fun onAnnotationDragStarted(annotation: Circle?) {
                decorations.findInputCallback<CircleNode, Circle, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation?.id },
                    nodeInputCallback = { onCircleDragged }
                )?.invoke(annotation!!)
            }

            override fun onAnnotationDrag(annotation: Circle?) {
                decorations.findInputCallback<CircleNode, Circle, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation?.id },
                    nodeInputCallback = { onCircleDragged }
                )?.invoke(annotation!!)
            }

            override fun onAnnotationDragFinished(annotation: Circle?) {
                decorations.findInputCallback<CircleNode, Circle, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation?.id },
                    nodeInputCallback = { onCircleDragStopped }
                )?.invoke(annotation!!)
            }
        })

        topCircleManager.addDragListener(object : OnCircleDragListener {
            override fun onAnnotationDragStarted(annotation: Circle?) {
                decorations.findInputCallback<CircleNode, Circle, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation?.id },
                    nodeInputCallback = { onCircleDragged }
                )?.invoke(annotation!!)
            }

            override fun onAnnotationDrag(annotation: Circle?) {
                decorations.findInputCallback<CircleNode, Circle, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation?.id },
                    nodeInputCallback = { onCircleDragged }
                )?.invoke(annotation!!)
            }

            override fun onAnnotationDragFinished(annotation: Circle?) {
                decorations.findInputCallback<CircleNode, Circle, Unit>(
                    nodeMatchPredicate = { it.circle.id == annotation?.id },
                    nodeInputCallback = { onCircleDragStopped }
                )?.invoke(annotation!!)
            }
        })
    }

    override fun insertBottomUp(index: Int, instance: MapNode) {
        decorations.add(index, instance)
        instance.onAttached()
    }

    override fun insertTopDown(index: Int, instance: MapNode) {
        decorations.add(index, instance)
        instance.onAttached()
    }

    override fun move(from: Int, to: Int, count: Int) {
        decorations.move(from, to, count)
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

internal class CircleNode(
    val circleManager: CircleManager,
    val circle: Circle,
    var onCircleDragged: (Circle) -> Unit,
    var onCircleDragStopped: (Circle) -> Unit
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
