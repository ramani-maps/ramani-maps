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

import androidx.compose.runtime.staticCompositionLocalOf

interface MapProjection {
    fun toScreenLocation(latLng: LatLng): ScreenPoint
    fun fromScreenLocation(point: ScreenPoint): LatLng
}

val LocalMapProjection = staticCompositionLocalOf<MapProjection> {
    error("MapProjection not provided. Map composables must be used inside a MapLibre { } block.")
}
