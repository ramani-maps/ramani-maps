package org.maplibre.compose

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng

@Composable
fun CoordToPixelMaper(coordinates: MutableList<LatLng>, onChange: (List<PointF>) -> Unit) {

    val mapApplier = currentComposer.applier as? MapApplier
    val projection = mapApplier?.map?.projection!!

    onChange(coordinates.map {
        projection.toScreenLocation(it)
    })

}