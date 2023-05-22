package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.imageResource
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property.TEXT_ANCHOR_CENTER
import com.mapbox.mapboxsdk.style.layers.Property.TEXT_JUSTIFY_CENTER

@Composable
@MapLibreComposable
fun Symbol(
    center: LatLng,
    size: Float,
    color: String,
    isDraggable: Boolean,
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

        var symbolOptions =
            SymbolOptions().withDraggable(isDraggable).withLatLng(center)


        imageId?.let {
            symbolOptions =
                symbolOptions.withIconImage("symbol").withIconColor(color).withIconSize(size)
        }

        text?.let {
            symbolOptions =
                symbolOptions.withTextField(text).withTextColor(color).withTextSize(size)
                    .withTextJustify(TEXT_JUSTIFY_CENTER).withTextAnchor(
                        TEXT_ANCHOR_CENTER
                    )
        }

        val symbol = mapApplier?.symbolManager!!.create(symbolOptions)
        SymbolNode(mapApplier?.symbolManager!!, symbol) {

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
    })
}