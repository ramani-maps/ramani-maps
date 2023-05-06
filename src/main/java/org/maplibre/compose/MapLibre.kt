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


suspend fun MapView.newComposition(
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

suspend fun MapboxMap.awaitStyle() = suspendCoroutine { continuation ->
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
    internal val mapView: MapView,
    val style: Style
) : AbstractApplier<MapNode>(MapNodeRoot) {

    private val decorations = mutableListOf<MapNode>()
    override fun insertBottomUp(index: Int, instance: MapNode) {
        decorations.add(index, instance)
        instance.onAttached()
    }

    override fun insertTopDown(index: Int, instance: MapNode) {
        decorations.add(index, instance)
        instance.onAttached()
    }

    override fun move(from: Int, to: Int, count: Int) {
        TODO("Not yet implemented")
    }

    override fun onClear() {
    }

    override fun remove(index: Int, count: Int) {
        TODO("Not yet implemented")
    }

}

internal class CircleNode(
    val circleManager: CircleManager,
    val circle: Circle,
    var onCircleClick: (Circle) -> Unit
) : MapNode {
    override fun onRemoved() {
    }
}

internal class SymbolNode(
    val symbolManager: SymbolManager,
    val symbol: Symbol,
    var onSymbolClicked: (Symbol) -> Unit
) : MapNode {
    override fun onRemoved() {
    }
}

internal class PolyLineNode(
    val lineManager: LineManager,
    val polyLine: Line,
    var onLineClicked: (Line) -> Unit
) : MapNode {
    override fun onRemoved() {
    }
}

internal class FillNode(
    val fillManager: FillManager,
    val fill: Fill,
    onFillClicked: (Fill) -> Unit
) : MapNode {
    override fun onRemoved() {

    }
}
