/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2023 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

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
    zIndex: Int = 0,
    imageId: Int? = null,
    imageRotation: Float? = null,
    text: String? = null
) {
    val mapApplier = currentComposer.applier as MapApplier

    imageId?.let {
        if (mapApplier.style.getImage("$imageId") == null) {
            mapApplier.style.addImage(
                "$imageId",
                ImageBitmap.imageResource(it).asAndroidBitmap()
            )
        }
    }

    ComposeNode<SymbolNode, MapApplier>(factory = {
        val symbolManager = mapApplier.getOrCreateSymbolManagerForZIndex(zIndex)
        var symbolOptions = SymbolOptions()
            .withDraggable(isDraggable)
            .withLatLng(center)

        imageId?.let {
            symbolOptions = symbolOptions
                .withIconImage(imageId.toString())
                .withIconColor(color)
                .withIconSize(size)
                .withIconRotate(imageRotation)
        }

        text?.let {
            symbolOptions = symbolOptions
                .withTextField(text)
                .withTextColor(color)
                .withTextSize(size)
                .withTextJustify(TEXT_JUSTIFY_CENTER)
                .withTextAnchor(TEXT_ANCHOR_CENTER)
        }

        val symbol = symbolManager.create(symbolOptions)
        SymbolNode(symbolManager, symbol)
    }, update = {
        set(center) {
            symbol.latLng = center
            symbolManager.update(symbol)
        }

        set(text) {
            symbol.textField = text
            symbolManager.update(symbol)
        }

        set(color) {
            symbol.iconColor = color
        }

        set(imageRotation) {
            symbol.iconRotate = imageRotation
        }
    })
}
