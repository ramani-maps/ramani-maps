package org.maplibre.compose

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng

@Composable
fun PixelToCoordMapper(points: MutableList<PointF>, onChange: (List<LatLng>) -> Unit) {

    val mapApplier = currentComposer.applier as? MapApplier
    val projection = mapApplier?.map?.projection!!

    onChange(points.map {
        projection.fromScreenLocation(it)
    })

}
