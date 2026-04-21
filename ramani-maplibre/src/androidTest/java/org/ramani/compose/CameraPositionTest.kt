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

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CameraPositionTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(TestActivity::class.java)

    private val blankStyle = """{"version": 8, "sources": {}, "layers": []}"""

    private fun withMap(block: (MapLibreMap) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        activityRule.scenario.onActivity { activity ->
            activity.mapView.getMapAsync { map ->
                map.setStyle(Style.Builder().fromJson(blankStyle)) {
                    try {
                        block(map)
                    } catch (t: Throwable) {
                        error = t
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }

        assertTrue("Test timed out", latch.await(15, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    // --- CameraPosition.toMapLibre() ---

    @Test
    fun toMapLibre_allFieldsNull_producesDefaultCameraPosition() {
        val pos = CameraPosition()
        val mlPos = pos.toMapLibre()

        // MapLibre's default is null target, 0.0 zoom, 0.0 tilt, 0.0 bearing
        assertNull("null target should stay null", mlPos.target)
    }

    @Test
    fun toMapLibre_targetSet_isPreserved() {
        val latLng = LatLng(48.5, 7.7)
        val pos = CameraPosition(target = latLng)
        val mlPos = pos.toMapLibre()

        assertEquals("latitude should be preserved", latLng.latitude, mlPos.target!!.latitude, 0.0001)
        assertEquals("longitude should be preserved", latLng.longitude, mlPos.target!!.longitude, 0.0001)
    }

    @Test
    fun toMapLibre_zoomSet_isPreserved() {
        val pos = CameraPosition(zoom = 12.5)
        val mlPos = pos.toMapLibre()
        assertEquals("zoom should be preserved", 12.5, mlPos.zoom, 0.0001)
    }

    @Test
    fun toMapLibre_tiltSet_isPreserved() {
        val pos = CameraPosition(tilt = 30.0)
        val mlPos = pos.toMapLibre()
        assertEquals("tilt should be preserved", 30.0, mlPos.tilt, 0.0001)
    }

    @Test
    fun toMapLibre_bearingSet_isPreserved() {
        val pos = CameraPosition(bearing = 90.0)
        val mlPos = pos.toMapLibre()
        assertEquals("bearing should be preserved", 90.0, mlPos.bearing, 0.0001)
    }

    @Test
    fun toMapLibre_allFieldsSet_allPreserved() {
        val pos = CameraPosition(
            target = LatLng(51.5, -0.12),
            zoom = 14.0,
            tilt = 45.0,
            bearing = 180.0,
        )
        val mlPos = pos.toMapLibre()

        assertNotNull(mlPos.target)
        assertEquals(51.5, mlPos.target!!.latitude, 0.0001)
        assertEquals(-0.12, mlPos.target!!.longitude, 0.0001)
        assertEquals(14.0, mlPos.zoom, 0.0001)
        assertEquals(45.0, mlPos.tilt, 0.0001)
        assertEquals(180.0, mlPos.bearing, 0.0001)
    }

    // --- CameraPosition equality ---

    @Test
    fun cameraPosition_sameFields_areEqual() {
        val a = CameraPosition(target = LatLng(48.5, 7.7), zoom = 10.0)
        val b = CameraPosition(target = LatLng(48.5, 7.7), zoom = 10.0)
        assertEquals("Camera positions with same fields should be equal", a, b)
    }

    @Test
    fun cameraPosition_differentTarget_areNotEqual() {
        val a = CameraPosition(target = LatLng(48.5, 7.7))
        val b = CameraPosition(target = LatLng(48.5, 7.8))
        org.junit.Assert.assertNotEquals("Camera positions with different targets should not be equal", a, b)
    }

    // --- MapLibreMap camera movement ---

    @Test
    fun moveCamera_toLatLng_updatesMapCameraPosition() = withMap { map ->
        val target = LatLng(48.5, 7.7)
        // newLatLng preserves the current zoom; at the default zoom=0 MapLibre cannot
        // place the camera at an arbitrary lat/lng, so set a valid zoom first.
        map.moveCamera(CameraUpdateFactory.zoomTo(10.0))
        map.moveCamera(CameraUpdateFactory.newLatLng(target))

        val actual = map.cameraPosition.target
        assertNotNull("Camera target should not be null after move", actual)
        assertEquals("Latitude should match", target.latitude, actual!!.latitude, 0.001)
        assertEquals("Longitude should match", target.longitude, actual.longitude, 0.001)
    }

    @Test
    fun moveCamera_withZoom_updatesZoom() = withMap { map ->
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(48.5, 7.7), 10.0))
        assertEquals("Zoom should match after moveCamera", 10.0, map.cameraPosition.zoom, 0.01)
    }

    @Test
    fun moveCamera_multipleTimes_keepsLastPosition() = withMap { map ->
        map.moveCamera(CameraUpdateFactory.zoomTo(10.0))
        map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(48.0, 7.0)))
        map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(51.5, -0.12)))

        val actual = map.cameraPosition.target!!
        assertEquals("Last latitude should win", 51.5, actual.latitude, 0.001)
        assertEquals("Last longitude should win", -0.12, actual.longitude, 0.001)
    }
}
