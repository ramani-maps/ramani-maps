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

actual val DefaultMarkerImage: Any = Unit

@Composable
actual fun Symbol(
    centerState: CenterState,
    size: Float,
    color: String,
    isDraggable: Boolean,
    layerId: String?,
    aboveLayerId: String?,
    belowLayerId: String?,
    imageId: Any?,
    imageAnchor: String,
    imageOffset: Array<Float>,
    imageRotation: Float?,
    text: String?,
    textAnchor: String,
    textJustify: String,
    textOffset: Array<Float>,
    textColor: String,
    textHaloColor: String,
    textHaloWidth: Float,
    data: Any?,
    onSymbolDragged: (LatLng) -> Unit,
    onDragFinished: (LatLng) -> Unit,
    onClick: (Any?) -> Unit,
    onLongClick: (Any?) -> Unit,
) {
    TODO("iOS implementation not yet available")
}
