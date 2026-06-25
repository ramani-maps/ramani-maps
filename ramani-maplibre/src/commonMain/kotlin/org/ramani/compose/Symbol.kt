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

/**
 * Platform-specific default marker image reference.
 *
 * On Android this resolves to the MapLibre default marker drawable resource ID.
 */
expect val DefaultMarkerImage: Any

@Composable
expect fun Symbol(
    centerState: CenterState,
    size: Float = 1F,
    color: String = "",
    isDraggable: Boolean = false,
    layerId: String? = null,
    aboveLayerId: String? = null,
    belowLayerId: String? = null,
    imageId: Any? = DefaultMarkerImage,
    sdf: Boolean = false,
    imageAnchor: String = "center",
    imageOffset: Array<Float> = arrayOf(0f, 0f),
    imageRotation: Float? = null,
    text: String? = null,
    textAnchor: String = "center",
    textJustify: String = "center",
    textOffset: Array<Float> = arrayOf(0f, 3f),
    textColor: String = "#000000",
    textHaloColor: String = "#000000",
    textHaloWidth: Float = 0f,
    data: Any? = null,
    onSymbolDragged: (LatLng) -> Unit = {},
    onDragFinished: (LatLng) -> Unit = {},
    onClick: (Any?) -> Unit = {},
    onLongClick: (Any?) -> Unit = {},
)
