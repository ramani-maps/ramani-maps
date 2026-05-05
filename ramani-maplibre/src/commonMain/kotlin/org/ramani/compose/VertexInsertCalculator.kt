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

@Composable
fun VertexInsertCalculator(
    vertices: List<LatLng>,
    onPointsChanged: (List<LatLng>) -> Unit
) {
    val projection = LocalMapProjection.current

    val vertexPixel = vertices.map { vertex ->
        projection.toScreenLocation(vertex)
    }

    val points = mutableListOf<ScreenPoint>()

    vertexPixel.forEachIndexed { index, point ->
        val next = if (index < vertexPixel.size - 1) {
            vertexPixel[index + 1]
        } else {
            vertexPixel[0]
        }

        val midpoint = ScreenPoint(
            x = point.x + (next.x - point.x) * 0.5f,
            y = point.y + (next.y - point.y) * 0.5f,
        )

        points.add(midpoint)
    }

    onPointsChanged(points.map {
        projection.fromScreenLocation(it)
    })
}
