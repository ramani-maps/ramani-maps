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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.maplibre.android.geometry.LatLng

class CircleCenterStateTest {

    @Test
    fun initialCenter_isReturned() {
        val initial = LatLng(48.5, 7.7)
        val state = CircleCenterState(initial)
        assertEquals(initial, state.center)
    }

    @Test
    fun setCenter_updatesValue() {
        val state = CircleCenterState(LatLng(0.0, 0.0))
        val newCenter = LatLng(51.5, -0.12)
        state.center = newCenter
        assertEquals(newCenter, state.center)
    }

    @Test
    fun updateCenterFromDrag_updatesCenter() {
        val state = CircleCenterState(LatLng(0.0, 0.0))
        val dragged = LatLng(44.0, 10.0)
        state.updateCenterFromDrag(dragged)
        assertEquals(dragged, state.center)
    }

    @Test
    fun saver_roundTrips() {
        val original = CircleCenterState(LatLng(48.5, 7.7))
        val saved = CircleCenterState.Saver.save(original)
        val restored = CircleCenterState.Saver.restore(saved!!)!!
        assertEquals(original.center, restored.center)
        assertNotSame(original, restored)
    }

    @Test
    fun saver_roundTrips_afterCenterChanged() {
        val state = CircleCenterState(LatLng(0.0, 0.0))
        state.center = LatLng(51.5, -0.12)

        val saved = CircleCenterState.Saver.save(state)
        val restored = CircleCenterState.Saver.restore(saved!!)!!
        assertEquals(LatLng(51.5, -0.12), restored.center)
    }
}
