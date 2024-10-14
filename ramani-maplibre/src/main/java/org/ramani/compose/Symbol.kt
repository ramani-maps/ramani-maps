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
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.plugins.annotation.OnSymbolDragListener
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_CENTER
import org.maplibre.android.style.layers.Property.TEXT_ANCHOR_CENTER
import org.maplibre.android.style.layers.Property.TEXT_JUSTIFY_CENTER

@Composable
@MapLibreComposable
fun Symbol(
    center: LatLng,
    size: Float,
    color: String,
    isDraggable: Boolean,
    onDrag: (LatLng) -> Unit = {},
    zIndex: Int = 0,
    imageId: Int? = null,
    imageAnchor: String = ICON_ANCHOR_CENTER,
    imageOffset: Array<Float> = arrayOf(0f, 0f),
    imageRotation: Float? = null,
    text: String? = null,
    textAnchor: String = TEXT_ANCHOR_CENTER,
    textJustify: String = TEXT_JUSTIFY_CENTER,
    textOffset: Array<Float> = arrayOf(0f, 3f),
    textColor: String = "#000000",
    textHaloColor: String = "#000000",
    textHaloWidth: Float = 0f
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
        }

        val symbol = symbolManager.create(symbolOptions)

        if (isDraggable) {
            symbolManager.addDragListener(
                object: OnSymbolDragListener {
                    override fun onAnnotationDragStarted(annotation: Symbol?) {

                    }

                    override fun onAnnotationDrag(annotation: Symbol?) {
                        if (annotation == symbol) {
                            annotation?.let {
                                onDrag(it.latLng)
                            }
                        }
                    }

                    override fun onAnnotationDragFinished(annotation: Symbol?) {

                    }
                }
            )
        }

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
