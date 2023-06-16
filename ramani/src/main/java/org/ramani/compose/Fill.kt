/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.FillOptions

@Composable
@MapLibreComposable
fun Fill(
    points: MutableList<LatLng>,
    fillColor: String = "Transparent",
    opacity: Float = 1.0f,
    zIndex: Int = 0,
    isDraggable: Boolean = false,
) {
    val mapApplier = currentComposer.applier as MapApplier

    ComposeNode<FillNode, MapApplier>(factory = {
        val fillManager = mapApplier.getOrCreateFillManagerForZIndex(zIndex)
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
