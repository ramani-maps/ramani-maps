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

import MapLibre.MLNMapView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake

@OptIn(ExperimentalForeignApi::class)
class MapProjectionIos(private val mapView: MLNMapView) : MapProjection {
    override fun toScreenLocation(latLng: LatLng): ScreenPoint {
        val point = mapView.convertCoordinate(latLng.toCLLocationCoordinate2D(), toPointToView = null)
        return point.useContents { ScreenPoint(x.toFloat(), y.toFloat()) }
    }

    override fun fromScreenLocation(point: ScreenPoint): LatLng {
        val coord = mapView.convertPoint(CGPointMake(point.x.toDouble(), point.y.toDouble()), toCoordinateFromView = null)
        return coord.toCommon()
    }
}
