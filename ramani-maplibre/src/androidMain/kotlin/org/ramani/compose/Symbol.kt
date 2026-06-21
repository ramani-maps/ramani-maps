/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.imageResource
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import org.maplibre.android.plugins.annotation.SymbolOptions

actual val DefaultMarkerImage: Any = org.maplibre.android.R.drawable.maplibre_marker_icon_default

@Composable
actual fun Symbol(
    centerState: CenterState,
    size: Float,
    color: String,
    isDraggable: Boolean,
    layerId: String?,
    aboveLayerId: String?,
    belowLayerId: String?,
    imageId: Any?,
    sdf: Boolean,
    imageAnchor: String,
    imageOffset: Array<Float>,
    imageRotation: Float?,
    text: String?,
    textAnchor: String,
    textJustify: String,
    textOffset: Array<Float>,
    textColor: String,
    textHaloColor: String,
    textHaloWidth: Float,
    data: Any?,
    onSymbolDragged: (LatLng) -> Unit,
    onDragFinished: (LatLng) -> Unit,
    onClick: (Any?) -> Unit,
    onLongClick: (Any?) -> Unit,
) {
    val mapApplier = LocalMapApplier.current
    val resolvedLayerId = layerId ?: remember { java.util.UUID.randomUUID().toString() }
    val jsonData = data as? JsonElement ?: JsonNull.INSTANCE
    val androidImageId = imageId as? Int

    androidImageId?.let {
        val bitmap = ImageBitmap.imageResource(it).asAndroidBitmap()
        try {
            val style = mapApplier.style.value
            if (style != null && style.getImage("$it") == null) {
                style.addImage("$it", bitmap, sdf)
            }
        } catch (_: IllegalStateException) {
            // Style is being replaced — image will be re-added after the new style loads
        }
    }

    ComposeNode<SymbolNode, MapApplier>(factory = {
        val symbolManager = mapApplier.getOrCreateSymbolManagerForLayerId(resolvedLayerId, aboveLayerId, belowLayerId)
        var symbolOptions = SymbolOptions()
            .withDraggable(isDraggable)
            .withLatLng(centerState.center.toMapLibre())
            .withData(jsonData)

        androidImageId?.let {
            symbolOptions = symbolOptions
                .withIconImage(it.toString())
                .withIconColor(color)
                .withIconSize(size)
                .withIconAnchor(imageAnchor)
                .withIconRotate(imageRotation)
                .withIconOffset(imageOffset)
        }

        text?.let {
            symbolOptions = symbolOptions
                .withTextField(text)
                .withTextColor(textColor)
                .withTextHaloColor(textHaloColor)
                .withTextHaloWidth(textHaloWidth)
                .withTextSize(size)
                .withTextJustify(textJustify)
                .withTextAnchor(textAnchor)
                .withTextOffset(textOffset)
            mapApplier.defaultFontNames()?.let { fonts ->
                symbolOptions = symbolOptions.withTextFont(fonts)
            }
        }

        val symbol = symbolManager.create(symbolOptions)

        SymbolNode(
            symbolManager,
            symbol,
            onSymbolDragged = {
                centerState.updateCenterFromDrag(it.latLng.toCommon())
                onSymbolDragged(it.latLng.toCommon())
            },
            onSymbolDragStopped = { onDragFinished(it.latLng.toCommon()) },
            onSymbolClicked = { onClick(it.data) },
            onSymbolLongClicked = { onLongClick(it.data) }
        )
    }, update = {
        update(onSymbolDragged) {
            this.onSymbolDragged = {
                centerState.updateCenterFromDrag(it.latLng.toCommon())
                onSymbolDragged(it.latLng.toCommon())
            }
        }

        update(onDragFinished) {
            this.onSymbolDragStopped = { onDragFinished(it.latLng.toCommon()) }
        }

        set(centerState.center) {
            symbol.latLng = centerState.center.toMapLibre()
            symbolManager.update(symbol)
        }

        set(text) {
            symbol.textField = text
            symbolManager.update(symbol)
        }

        set(color) {
            symbol.iconColor = color
            symbolManager.update(symbol)
        }

        set(imageRotation) {
            symbol.iconRotate = imageRotation
            symbolManager.update(symbol)
        }
    })
}
