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
import androidx.compose.runtime.remember
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.plugins.annotation.FillOptions

@Composable
fun Fill(
    points: List<LatLng>,
    fillColor: String = "Transparent",
    opacity: Float = 1.0f,
    layerId: String? = null,
    aboveLayerId: String? = null,
    belowLayerId: String? = null,
    isDraggable: Boolean = false,
) {
    val mapApplier = currentComposer.applier as MapApplier
    val resolvedLayerId = layerId ?: remember { java.util.UUID.randomUUID().toString() }

    ComposeNode<FillNode, MapApplier>(factory = {
        val fillManager = mapApplier.getOrCreateFillManagerForLayerId(resolvedLayerId, aboveLayerId, belowLayerId)
        val fillOptions = FillOptions()
            .withLatLngs(mutableListOf(points))
            .withFillColor(fillColor)
            .withFillOpacity(opacity)
            .withDraggable(isDraggable)
        val fill = fillManager.create(fillOptions)

        FillNode(fillManager, fill)
    }, update = {
        set(points) {
            fill.latLngs = mutableListOf(points)
            fillManager.update(fill)
        }

        set(fillColor) {
            fill.fillColor = fillColor
            fillManager.update(fill)
        }

        set(opacity) {
            fill.fillOpacity = opacity
            fillManager.update(fill)
        }
    })
}
