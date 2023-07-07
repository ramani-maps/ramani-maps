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
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions

@Composable
@MapboxComposable
fun Polyline(
    points: List<LatLng>,
    color: String,
    lineWidth: Float,
    zIndex: Int = 0,
    isDraggable: Boolean = false
) {
    val mapApplier = currentComposer.applier as MapApplier

    ComposeNode<PolylineNode, MapApplier>(factory = {
        val lineManager = mapApplier.getOrCreateLineManagerForZIndex(zIndex)
        val lineOptions = PolylineAnnotationOptions()
            .withPoints(points.map { Point.fromLngLat(it.longitude, it.latitude) })
            .withLineColor(color)
            .withLineWidth(lineWidth.toDouble())
            .withDraggable(isDraggable)
        val polyLine = lineManager.create(lineOptions)

        PolylineNode(lineManager, polyLine)
    }, update = {
        set(points) {
            polyLine.points = points.map { Point.fromLngLat(it.longitude, it.latitude) }
            lineManager.update(polyLine)
        }

        set(color) {
            polyLine.lineColorString = color
            lineManager.update(polyLine)
        }

        set(lineWidth) {
            polyLine.lineWidth = lineWidth.toDouble()
        }
    })
}
