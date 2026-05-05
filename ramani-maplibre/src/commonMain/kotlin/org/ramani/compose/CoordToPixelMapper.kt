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
fun CoordToPixelMapper(coordinates: MutableList<LatLng>, onChange: (List<ScreenPoint>) -> Unit) {
    val projection = LocalMapProjection.current

    onChange(coordinates.map {
        projection.toScreenLocation(it)
    })
}

@Composable
fun pixelFromCoord(coord: LatLng): ScreenPoint {
    val projection = LocalMapProjection.current

    return projection.toScreenLocation(coord)
}

@Composable
fun coordFromPixel(point: ScreenPoint): LatLng {
    val projection = LocalMapProjection.current

    return projection.fromScreenLocation(point)
}
