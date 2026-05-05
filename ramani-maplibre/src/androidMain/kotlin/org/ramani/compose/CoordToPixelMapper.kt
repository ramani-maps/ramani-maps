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

import android.graphics.PointF
import androidx.compose.runtime.Composable
@Composable
fun CoordToPixelMapper(coordinates: MutableList<LatLng>, onChange: (List<PointF>) -> Unit) {
    val mapApplier = LocalMapApplier.current
    val projection = mapApplier.map.projection

    onChange(coordinates.map {
        projection.toScreenLocation(it.toMapLibre())
    })
}

@Composable
fun pixelFromCoord(coord: LatLng): PointF {
    val mapApplier = LocalMapApplier.current
    val projection = mapApplier.map.projection

    return projection.toScreenLocation(coord.toMapLibre())
}

@Composable
fun coordFromPixel(point: PointF): LatLng {
    val mapApplier = LocalMapApplier.current
    val projection = mapApplier.map.projection

    return projection.fromScreenLocation(point).toCommon()
}
