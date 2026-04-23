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
import androidx.compose.runtime.remember
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.plugins.annotation.LineOptions

@Composable
fun Polyline(
    points: List<LatLng>,
    color: String,
    lineWidth: Float,
    layerId: String? = null,
    aboveLayerId: String? = null,
    belowLayerId: String? = null,
    isDraggable: Boolean = false,
    dashType: Array<Float>? = null,
) {
    val mapApplier = LocalMapApplier.current
    val resolvedLayerId = layerId ?: remember { java.util.UUID.randomUUID().toString() }

    ComposeNode<PolyLineNode, MapApplier>(factory = {
        val lineManager = mapApplier.getOrCreateLineManagerForLayerId(resolvedLayerId, aboveLayerId, belowLayerId)
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
            lineManager.update(polyLine)
        }
    })
}
