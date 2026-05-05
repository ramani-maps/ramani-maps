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
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import org.maplibre.android.plugins.annotation.CircleOptions

@Composable
actual fun Circle(
    centerState: CenterState,
    radius: Float,
    isDraggable: Boolean,
    color: String,
    opacity: Float,
    borderColor: String,
    borderWidth: Float,
    layerId: String?,
    aboveLayerId: String?,
    belowLayerId: String?,
    data: Any?,
    onCenterDragged: (LatLng) -> Unit,
    onDragFinished: (LatLng) -> Unit,
    onClick: (Any?) -> Unit,
    onLongClick: (Any?) -> Unit,
) {
    val mapApplier = LocalMapApplier.current
    val resolvedLayerId = layerId ?: remember { java.util.UUID.randomUUID().toString() }
    val jsonData = data as? JsonElement ?: JsonNull.INSTANCE

    ComposeNode<CircleNode, MapApplier>(factory = {
        val circleManager = mapApplier.getOrCreateCircleManagerForLayerId(resolvedLayerId, aboveLayerId, belowLayerId)

        val circleOptions = CircleOptions()
            .withCircleRadius(radius)
            .withLatLng(centerState.center.toMapLibre())
            .withDraggable(isDraggable)
            .withCircleStrokeColor(borderColor)
            .withCircleStrokeWidth(borderWidth)
            .withCircleOpacity(opacity)
            .withData(jsonData)

        val circle = circleManager.create(circleOptions)

        CircleNode(
            circleManager,
            circle,
            onCircleDragged = {
                centerState.updateCenterFromDrag(it.latLng.toCommon())
                onCenterDragged(it.latLng.toCommon())
            },
            onCircleDragStopped = { onDragFinished(it.latLng.toCommon()) },
            onCircleClicked = { onClick(it.data) },
            onCircleLongClicked = { onLongClick(it.data) }
        )
    }, update = {
        update(onCenterDragged) {
            this.onCircleDragged = {
                centerState.updateCenterFromDrag(it.latLng.toCommon())
                onCenterDragged(it.latLng.toCommon())
            }
        }

        update(onDragFinished) {
            this.onCircleDragStopped = { onDragFinished(it.latLng.toCommon()) }
        }

        set(centerState.center) {
            circle.latLng = centerState.center.toMapLibre()
            circleManager.update(circle)
        }

        set(color) {
            circle.circleColor = color
            circleManager.update(circle)
        }

        set(radius) {
            circle.circleRadius = radius
            circleManager.update(circle)
        }
    })
}
