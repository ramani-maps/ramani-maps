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
import com.mapbox.maps.ScreenCoordinate

@Composable
fun pixelFromCoord(coord: LatLng): ScreenCoordinate {
    val mapApplier = currentComposer.applier as MapApplier
    return mapApplier.map.pixelForCoordinate(Point.fromLngLat(coord.longitude, coord.latitude))
}

@Composable
fun coordFromPixel(point: ScreenCoordinate): LatLng {
    val mapApplier = currentComposer.applier as MapApplier
    val coord = mapApplier.map.coordinateForPixel(point)
    return LatLng(coord.latitude(), coord.longitude())
}
