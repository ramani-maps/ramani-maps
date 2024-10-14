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
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.plugins.annotation.LineOptions

enum class PolylineDashType(val dashArray: Array<Float>?) {
    NoDash(null),
    Dash(arrayOf(2f, 1f)),
    LongDash(arrayOf(3f, 1f));
    
    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.ordinal == value } ?: NoDash
    }
}

@Composable
@MapLibreComposable
fun Polyline(
    points: List<LatLng>,
    color: String,
    lineWidth: Float,
    zIndex: Int = 0,
    isDraggable: Boolean = false,
    dashType: Array<Float>? = PolylineDashType.NoDash.dashArray,
) {
    val mapApplier = currentComposer.applier as MapApplier

    ComposeNode<PolyLineNode, MapApplier>(factory = {
        val lineManager = mapApplier.getOrCreateLineManagerForZIndex(zIndex)
        val lineOptions = LineOptions()
            .withLatLngs(points)
            .withLineColor(color)
            .withLineWidth(lineWidth)
            .withDraggable(isDraggable)

        lineManager.lineDasharray = dashType

        val polyLine = lineManager.create(lineOptions)
        PolyLineNode(lineManager, polyLine)
    }, update = {
        set(points) {
            polyLine.latLngs = points
            lineManager.update(polyLine)
        }

        set(color) {
            polyLine.lineColor = color
            lineManager.update(polyLine)
        }

        set(lineWidth) {
            polyLine.lineWidth = lineWidth
        }
    })
}
