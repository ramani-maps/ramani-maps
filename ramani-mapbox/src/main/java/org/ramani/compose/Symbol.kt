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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.platform.LocalContext
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextJustify
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions

@Composable
@MapboxComposable
fun Symbol(
    center: LatLng,
    size: Float,
    color: String,
    isDraggable: Boolean,
    zIndex: Int = 0,
    imageId: Int? = null,
    text: String? = null
) {
    val context = LocalContext.current
    val mapApplier = currentComposer.applier as MapApplier

    ComposeNode<SymbolNode, MapApplier>(factory = {
        val symbolManager = mapApplier.getOrCreateSymbolManagerForZIndex(zIndex)
        var symbolOptions = PointAnnotationOptions()
            .withDraggable(isDraggable)
            .withPoint(Point.fromLngLat(center.longitude, center.latitude))

        imageId?.let { imageId ->
            bitmapFromDrawableRes(context, imageId)?.let { bitmap ->
                symbolOptions = symbolOptions
                    .withIconImage(bitmap)
                    .withIconColor(color)
                    .withIconSize(size.toDouble())
            }
        }

        text?.let {
            symbolOptions = symbolOptions
                .withTextField(text)
                .withTextColor(color)
                .withTextSize(size.toDouble())
                .withTextJustify(TextJustify.CENTER)
                .withTextAnchor(TextAnchor.CENTER)
        }

        val symbol = symbolManager.create(symbolOptions)
        SymbolNode(symbolManager, symbol)
    }, update = {
        set(center) {
            symbol.point = Point.fromLngLat(center.longitude, center.latitude)
            symbolManager.update(symbol)
        }

        set(text) {
            symbol.textField = text
            symbolManager.update(symbol)
        }

        set(color) {
            symbol.iconColorString = color
        }
    })
}

private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
    convertDrawableToBitmap(context.getDrawable(resourceId))

private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
    if (sourceDrawable == null) {
        return null
    }
    return if (sourceDrawable is BitmapDrawable) {
        sourceDrawable.bitmap
    } else {
        // copying drawable object to not manipulate on the same reference
        val constantState = sourceDrawable.constantState ?: return null
        val drawable = constantState.newDrawable().mutate()
        val bitmap: Bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    }
}
