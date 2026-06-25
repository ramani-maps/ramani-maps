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

import kotlin.math.sqrt

data class ScreenPoint(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: ScreenPoint) = ScreenPoint(x + other.x, y + other.y)
    operator fun minus(other: ScreenPoint) = ScreenPoint(x - other.x, y - other.y)
    operator fun times(scalar: Float) = ScreenPoint(x * scalar, y * scalar)
    fun length() = sqrt(x * x + y * y)
}
