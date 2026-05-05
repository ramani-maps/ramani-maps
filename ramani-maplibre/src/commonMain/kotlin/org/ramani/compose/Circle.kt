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
expect fun Circle(
    centerState: CenterState,
    radius: Float,
    isDraggable: Boolean = false,
    color: String = "Yellow",
    opacity: Float = 1.0f,
    borderColor: String = "Black",
    borderWidth: Float = 0.0F,
    layerId: String? = null,
    aboveLayerId: String? = null,
    belowLayerId: String? = null,
    data: Any? = null,
    onCenterDragged: (LatLng) -> Unit = {},
    onDragFinished: (LatLng) -> Unit = {},
    onClick: (Any?) -> Unit = {},
    onLongClick: (Any?) -> Unit = {},
)
