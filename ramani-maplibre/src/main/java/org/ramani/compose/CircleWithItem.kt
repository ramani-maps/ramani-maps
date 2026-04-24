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
import androidx.compose.runtime.remember
import org.maplibre.android.geometry.LatLng

@Composable
fun CircleWithItem(
    center: LatLng,
    radius: Float,
    dragRadius: Float = radius,
    isDraggable: Boolean,
    color: String,
    borderColor: String = "Black",
    borderWidth: Float = 0.0f,
    opacity: Float = 1.0f,
    layerId: String? = null,
    aboveLayerId: String? = null,
    imageId: Int? = null,
    itemSize: Float = 0.0f,
    text: String? = null,
    onCenterChanged: (LatLng) -> Unit = {},
    onDragStopped: () -> Unit = {},
) {
    val resolvedLayerId = layerId ?: remember { java.util.UUID.randomUUID().toString() }
    val dragCenterState = remember { CircleCenterState(center) }
    val displayCenterState = remember { CircleCenterState(center) }

    dragCenterState.center = center
    displayCenterState.center = center

    // Invisible circle, this is the draggable
    Circle(
        centerState = dragCenterState,
        radius = dragRadius,
        isDraggable = isDraggable,
        color = "Transparent",
        borderColor = borderColor,
        borderWidth = 0.0f,
        layerId = "${resolvedLayerId}_drag",
        aboveLayerId = resolvedLayerId,
        onCenterDragged = {
            onCenterChanged(it)
        },
        onDragFinished = {
            dragCenterState.center = center
            onDragStopped()
        },
    )

    // Display circle
    Circle(
        centerState = displayCenterState,
        radius = radius,
        isDraggable = false,
        color = color,
        opacity = opacity,
        layerId = resolvedLayerId,
        aboveLayerId = aboveLayerId,
        borderColor = borderColor,
        borderWidth = borderWidth,
    )

    imageId?.let {
        Symbol(
            center = center,
            color = "Black",
            isDraggable = false,
            imageId = imageId,
            size = itemSize,
            layerId = "${resolvedLayerId}_image",
            aboveLayerId = resolvedLayerId,
        )
    }

    text?.let {
        Symbol(
            center = center,
            color = "Black",
            isDraggable = false,
            text = text,
            size = itemSize,
            layerId = "${resolvedLayerId}_text",
            aboveLayerId = resolvedLayerId,
            imageId = null
        )
    }
}
