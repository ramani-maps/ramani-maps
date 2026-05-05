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

import org.maplibre.android.maps.Projection

class MapProjectionAndroid(private val projection: Projection) : MapProjection {
    override fun toScreenLocation(latLng: LatLng): ScreenPoint {
        val point = projection.toScreenLocation(latLng.toMapLibre())
        return ScreenPoint(point.x, point.y)
    }

    override fun fromScreenLocation(point: ScreenPoint): LatLng {
        val latLng = projection.fromScreenLocation(android.graphics.PointF(point.x, point.y))
        return latLng.toCommon()
    }
}
