package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.core.graphics.minus
import com.mapbox.mapboxsdk.geometry.LatLng

@MapLibreComposable
@Composable
fun ScreenDistanceBetween(a: LatLng, b: LatLng): Float {

    val mapApplier = currentComposer.applier as? MapApplier
    val projection = mapApplier?.map?.projection!!

    val pixelA = projection.toScreenLocation(a)
    val pixelB = projection.toScreenLocation(b)

    return (pixelB - pixelA).length()


}