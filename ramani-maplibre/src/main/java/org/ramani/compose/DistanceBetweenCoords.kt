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
import androidx.core.graphics.minus
import org.maplibre.android.geometry.LatLng

@Composable
fun screenDistanceBetween(a: LatLng, b: LatLng): Float {
    val mapApplier = LocalMapApplier.current
    val projection = mapApplier.map.projection

    val pixelA = projection.toScreenLocation(a)
    val pixelB = projection.toScreenLocation(b)

    return (pixelB - pixelA).length()
}
