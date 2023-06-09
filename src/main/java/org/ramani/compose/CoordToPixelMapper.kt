package org.ramani.compose

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng

@Composable
fun CoordToPixelMapper(coordinates: MutableList<LatLng>, onChange: (List<PointF>) -> Unit) {
    val mapApplier = currentComposer.applier as MapApplier
    val projection = mapApplier.map.projection

    onChange(coordinates.map {
        projection.toScreenLocation(it)
    })
}

@Composable
fun pixelFromCoord(coord: LatLng): PointF {
    val mapApplier = currentComposer.applier as MapApplier
    val projection = mapApplier.map.projection

    return projection.toScreenLocation(coord)
}

@Composable
fun coordFromPixel(point: PointF): LatLng {
    val mapApplier = currentComposer.applier as MapApplier
    val projection = mapApplier.map.projection

    return projection.fromScreenLocation(point)
}
