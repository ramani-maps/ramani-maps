package org.maplibre.compose

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.core.graphics.minus
import androidx.core.graphics.plus
import com.mapbox.mapboxsdk.geometry.LatLng

@MapLibreComposable
@Composable
fun VerticeInsertCalculator(
    vertices: MutableList<LatLng>,
    onPointsChanged: (List<LatLng>) -> Unit
) {
    val mapApplier = currentComposer.applier as? MapApplier
    val projection = mapApplier?.map?.projection!!


    val verticePixel = vertices.map { vertice ->
        projection.toScreenLocation(vertice)
    }

    val points = mutableListOf<PointF>()

    verticePixel.forEachIndexed { index, point ->
        var tmp = PointF()
        if (index < verticePixel.size - 1) {
            tmp = (verticePixel[index + 1] - point)
        } else {
            // last item
            tmp = verticePixel[0] - point
        }
        tmp.x = tmp.x * 0.5f
        tmp.y = tmp.y * 0.5f
        points.add(point + tmp)
    }

    onPointsChanged(points.map {
        projection.fromScreenLocation(it)
    }
    )
}