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

data class UserLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val bearing: Float = 0f,
    /** Horizontal accuracy radius in meters, or null when the fix doesn't report one. */
    val accuracy: Float? = null,
)
