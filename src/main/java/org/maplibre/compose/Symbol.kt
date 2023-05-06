package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.utils.BitmapUtils

@Composable
@MapLibreComposable
fun Symbol(center : LatLng, size: Float, color : String, draggable : Boolean, imageId: Int) {

    val mapApplier = currentComposer.applier as? MapApplier

    if (mapApplier?.style != null) {
        mapApplier?.style!!.addImage("symbol",
            ImageBitmap.imageResource(id = imageId)
                .asAndroidBitmap()
        )
    }
    ComposeNode<SymbolNode, MapApplier>(factory = {
        val symbolManager =
            SymbolManager(mapApplier?.mapView!!, mapApplier?.map!!, mapApplier?.style!!)

        val symbolOptions =
            SymbolOptions().withDraggable(draggable).withLatLng(center).withIconImage("symbol").withIconColor(color).withIconSize(size)
        val symbol = symbolManager.create(symbolOptions)
        SymbolNode(symbolManager, symbol) {

        }
    }, update = {
        set(center) {
            symbol.latLng = center
            symbolManager.update(symbol)
        }
    }) {}
}