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
import androidx.compose.runtime.currentComposer
import com.mapbox.geojson.Point

@MapboxComposable
@Composable
fun screenDistanceBetween(a: LatLng, b: LatLng): Float {
    val mapApplier = currentComposer.applier as MapApplier

    val pixelA = mapApplier.map.pixelForCoordinate(Point.fromLngLat(a.longitude, a.latitude))
    val pixelB = mapApplier.map.pixelForCoordinate(Point.fromLngLat(b.longitude, b.latitude))

    return (pixelB - pixelA).length()
}
