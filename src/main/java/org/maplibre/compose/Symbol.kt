package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.imageResource
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property.TEXT_ANCHOR_CENTER
import com.mapbox.mapboxsdk.style.layers.Property.TEXT_JUSTIFY_CENTER

@Composable
@MapLibreComposable
fun Symbol(
    center: LatLng,
    size: Float,
    color: String,
    draggable: Boolean,
    imageId: Int? = null,
    text: String? = null
) {

    val mapApplier = currentComposer.applier as? MapApplier

    if (mapApplier?.style != null && imageId != null) {
        mapApplier?.style!!.addImage(
            "symbol",
            ImageBitmap.imageResource(id = imageId)
                .asAndroidBitmap()
        )
    }
    ComposeNode<SymbolNode, MapApplier>(factory = {
        val symbolManager =
            SymbolManager(mapApplier?.mapView!!, mapApplier?.map!!, mapApplier?.style!!)

        var symbolOptions =
            SymbolOptions().withDraggable(draggable).withLatLng(center)


        if (imageId != null) {
            symbolOptions =
                symbolOptions.withIconImage("symbol").withIconColor(color).withIconSize(size)
        } else if (text != null) {
            symbolOptions =
                symbolOptions.withTextField(text).withTextColor(color).withTextSize(size)
                    .withTextJustify(TEXT_JUSTIFY_CENTER).withTextAnchor(
                        TEXT_ANCHOR_CENTER
                    )
        }

        val symbol = symbolManager.create(symbolOptions)
        SymbolNode(symbolManager, symbol) {

        }
    }, update = {
        set(center) {
            symbol.latLng = center
            symbolManager.update(symbol)
        }
        set(text) {
            symbol.textField = text
            symbolManager.update(symbol)
        }
    }) {}
}