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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.maplibre.android.geometry.LatLng

/**
 * A state holder for a polygon's geometry, providing bidirectional synchronisation
 * between user code and the map:
 *
 * - **User → map:** Setting [vertices] or [azimuth] updates the polygon on the map.
 * - **Map → user:** Drag-driven changes are reflected in [vertices] and [azimuth]
 *   so that composables reading them recompose automatically.
 *
 * [vertexStates] exposes [CenterState] objects for each vertex. Pass them directly
 * to [Circle] composables to get draggable vertex handles that stay in sync with
 * the polygon — no manual synchronisation needed.
 *
 * [center] is derived from [vertices] (geographic centroid) and is read-only.
 *
 * Use [rememberPolygonState] to create an instance that survives recomposition
 * and configuration changes.
 */
class PolygonState(
    initialVertices: List<LatLng>,
    initialAzimuth: Float = 0f,
) {
    val vertexStates: List<CenterState> = initialVertices.map { CenterState(it) }

    var vertices: List<LatLng>
        get() = vertexStates.map { it.center }
        set(value) {
            value.forEachIndexed { i, v -> vertexStates[i].center = v }
        }

    var azimuth by mutableFloatStateOf(initialAzimuth)

    val center: LatLng
        get() {
            if (vertexStates.isEmpty()) return LatLng()
            val lat = vertexStates.sumOf { it.center.latitude } / vertexStates.size
            val lng = vertexStates.sumOf { it.center.longitude } / vertexStates.size
            return LatLng(lat, lng)
        }

    internal fun updateVerticesFromDrag(newVertices: List<LatLng>) {
        newVertices.forEachIndexed { i, v ->
            vertexStates[i].updateCenterFromDrag(v)
        }
    }

    internal fun updateAzimuthFromDrag(newAzimuth: Float) {
        azimuth = newAzimuth
    }

    companion object {
        val Saver: Saver<PolygonState, ArrayList<Double>> = Saver(
            save = { state ->
                ArrayList<Double>().apply {
                    add(state.azimuth.toDouble())
                    state.vertexStates.forEach {
                        add(it.center.latitude)
                        add(it.center.longitude)
                    }
                }
            },
            restore = { list ->
                val azimuth = list[0].toFloat()
                val vertices = (1 until list.size step 2).map { i ->
                    LatLng(list[i], list[i + 1])
                }
                PolygonState(vertices, azimuth)
            }
        )
    }
}

/**
 * Creates and remembers a [PolygonState] that survives recomposition
 * and configuration changes.
 *
 * @param vertices The initial polygon vertices.
 * @param azimuth The initial azimuth (rotation) in radians.
 */
@Composable
fun rememberPolygonState(
    vertices: List<LatLng>,
    azimuth: Float = 0f,
): PolygonState = rememberSaveable(saver = PolygonState.Saver) {
    PolygonState(vertices, azimuth)
}
