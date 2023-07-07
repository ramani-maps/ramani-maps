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

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.core.graphics.minus
import androidx.core.graphics.plus
import com.mapbox.mapboxsdk.geometry.LatLng

@MapLibreComposable
@Composable
fun VertexInsertCalculator(
    vertices: List<LatLng>,
    onPointsChanged: (List<LatLng>) -> Unit
) {
    val mapApplier = currentComposer.applier as MapApplier
    val projection = mapApplier.map.projection

    val vertexPixel = vertices.map { vertex ->
        projection.toScreenLocation(vertex)
    }

    val points = mutableListOf<PointF>()

    vertexPixel.forEachIndexed { index, point ->
        val tmp = if (index < vertexPixel.size - 1) {
            vertexPixel[index + 1] - point
        } else {
            // last item
            vertexPixel[0] - point
        }

        tmp.x *= 0.5f
        tmp.y *= 0.5f

        points.add(point + tmp)
    }

    onPointsChanged(points.map {
        projection.fromScreenLocation(it)
    })
}

