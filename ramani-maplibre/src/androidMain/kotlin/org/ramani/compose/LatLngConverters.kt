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

import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import org.maplibre.android.geometry.LatLngBounds as MapLibreLatLngBounds

fun LatLng.toMapLibre(): MapLibreLatLng =
    MapLibreLatLng(latitude, longitude, altitude)

fun MapLibreLatLng.toCommon(): LatLng =
    LatLng(latitude, longitude, altitude)

fun LatLngBounds.toMapLibre(): MapLibreLatLngBounds =
    MapLibreLatLngBounds.Builder()
        .include(northeast.toMapLibre())
        .include(southwest.toMapLibre())
        .build()

fun MapLibreLatLngBounds.toCommon(): LatLngBounds =
    LatLngBounds(
        northeast = northEast.toCommon(),
        southwest = southWest.toCommon(),
    )
