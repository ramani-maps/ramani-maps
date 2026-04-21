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

import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.CircleOptions
import org.maplibre.android.plugins.annotation.FillOptions
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.plugins.annotation.SymbolOptions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AnnotationLifecycleTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(TestActivity::class.java)

    private val blankStyle = """{"version": 8, "sources": {}, "layers": []}"""

    private fun withApplier(block: (MapApplier) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        activityRule.scenario.onActivity { activity ->
            activity.mapView.getMapAsync { map ->
                map.setStyle(Style.Builder().fromJson(blankStyle)) { style ->
                    try {
                        block(MapApplier(map, activity.mapView, mutableStateOf(style)))
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

    // --- Circle ---

    @Test
    fun circle_createAddsAnnotationToManager() = withApplier { applier ->
        val manager = applier.getOrCreateCircleManagerForLayerId("circles", null, null)
        assertEquals("No annotations before create", 0, manager.annotations.size())

        manager.create(CircleOptions().withLatLng(LatLng(48.0, 7.0)))

        assertEquals("One annotation after create", 1, manager.annotations.size())
    }

    @Test
    fun circleNode_onRemoved_deletesAnnotation() = withApplier { applier ->
        val manager = applier.getOrCreateCircleManagerForLayerId("circles", null, null)
        val circle = manager.create(CircleOptions().withLatLng(LatLng(48.0, 7.0)))
        assertEquals(1, manager.annotations.size())

        val node = CircleNode(manager, circle, {}, {}, {}, {})
        node.onRemoved()

        assertEquals("Annotation should be deleted after onRemoved", 0, manager.annotations.size())
    }

    @Test
    fun circleNode_onCleared_deletesAnnotation() = withApplier { applier ->
        val manager = applier.getOrCreateCircleManagerForLayerId("circles", null, null)
        val circle = manager.create(CircleOptions().withLatLng(LatLng(48.0, 7.0)))

        val node = CircleNode(manager, circle, {}, {}, {}, {})
        node.onCleared()

        assertEquals("Annotation should be deleted after onCleared", 0, manager.annotations.size())
    }

    @Test
    fun multipleCircles_onSameLayer_allLiveOnOneManager() = withApplier { applier ->
        val manager = applier.getOrCreateCircleManagerForLayerId("circles", null, null)

        manager.create(CircleOptions().withLatLng(LatLng(48.0, 7.0)))
        manager.create(CircleOptions().withLatLng(LatLng(49.0, 8.0)))
        manager.create(CircleOptions().withLatLng(LatLng(50.0, 9.0)))

        assertEquals("All circles should be on the same manager", 3, manager.annotations.size())
    }

    @Test
    fun deletingOneCircle_doesNotAffectOthers() = withApplier { applier ->
        val manager = applier.getOrCreateCircleManagerForLayerId("circles", null, null)

        val first = manager.create(CircleOptions().withLatLng(LatLng(48.0, 7.0)))
        manager.create(CircleOptions().withLatLng(LatLng(49.0, 8.0)))
        assertEquals(2, manager.annotations.size())

        CircleNode(manager, first, {}, {}, {}, {}).onRemoved()

        assertEquals("Only one circle should remain", 1, manager.annotations.size())
    }

    // --- Polyline ---

    @Test
    fun polyline_createAddsAnnotationToManager() = withApplier { applier ->
        val manager = applier.getOrCreateLineManagerForLayerId("lines", null, null)
        assertEquals(0, manager.annotations.size())

        manager.create(
            LineOptions()
                .withLatLngs(listOf(LatLng(48.0, 7.0), LatLng(49.0, 8.0)))
                .withLineColor("#FF0000")
                .withLineWidth(2f)
        )

        assertEquals("One line after create", 1, manager.annotations.size())
    }

    @Test
    fun polylineNode_onRemoved_deletesAnnotation() = withApplier { applier ->
        val manager = applier.getOrCreateLineManagerForLayerId("lines", null, null)
        val line = manager.create(
            LineOptions()
                .withLatLngs(listOf(LatLng(48.0, 7.0), LatLng(49.0, 8.0)))
                .withLineColor("#FF0000")
                .withLineWidth(2f)
        )

        PolyLineNode(manager, line).onRemoved()

        assertEquals("Line should be deleted after onRemoved", 0, manager.annotations.size())
    }

    // --- Fill ---

    @Test
    fun fill_createAddsAnnotationToManager() = withApplier { applier ->
        val manager = applier.getOrCreateFillManagerForLayerId("fills", null, null)
        assertEquals(0, manager.annotations.size())

        manager.create(
            FillOptions()
                .withLatLngs(
                    mutableListOf(
                        listOf(
                            LatLng(48.0, 7.0), LatLng(48.0, 8.0),
                            LatLng(49.0, 8.0), LatLng(49.0, 7.0)
                        )
                    )
                )
                .withFillColor("#0000FF")
        )

        assertEquals("One fill after create", 1, manager.annotations.size())
    }

    @Test
    fun fillNode_onRemoved_deletesAnnotation() = withApplier { applier ->
        val manager = applier.getOrCreateFillManagerForLayerId("fills", null, null)
        val fill = manager.create(
            FillOptions()
                .withLatLngs(
                    mutableListOf(
                        listOf(
                            LatLng(48.0, 7.0), LatLng(48.0, 8.0),
                            LatLng(49.0, 8.0), LatLng(49.0, 7.0)
                        )
                    )
                )
                .withFillColor("#0000FF")
        )

        FillNode(manager, fill).onRemoved()

        assertEquals("Fill should be deleted after onRemoved", 0, manager.annotations.size())
    }

    // --- Symbol ---

    @Test
    fun symbol_createAddsAnnotationToManager() = withApplier { applier ->
        val manager = applier.getOrCreateSymbolManagerForLayerId("symbols", null, null)
        assertEquals(0, manager.annotations.size())

        manager.create(SymbolOptions().withLatLng(LatLng(48.0, 7.0)))

        assertEquals("One symbol after create", 1, manager.annotations.size())
    }

    @Test
    fun symbolNode_onRemoved_deletesAnnotation() = withApplier { applier ->
        val manager = applier.getOrCreateSymbolManagerForLayerId("symbols", null, null)
        val symbol = manager.create(SymbolOptions().withLatLng(LatLng(48.0, 7.0)))

        SymbolNode(manager, symbol, {}, {}, {}, {}).onRemoved()

        assertEquals("Symbol should be deleted after onRemoved", 0, manager.annotations.size())
    }

    // --- Annotation update ---

    @Test
    fun circle_updatingPosition_reflectsInAnnotation() = withApplier { applier ->
        val manager = applier.getOrCreateCircleManagerForLayerId("circles", null, null)
        val circle = manager.create(CircleOptions().withLatLng(LatLng(48.0, 7.0)))

        val newPosition = LatLng(51.5, -0.12)
        circle.latLng = newPosition
        manager.update(circle)

        val stored = manager.annotations.get(circle.id)
        assertEquals("Updated latitude should be reflected", newPosition.latitude, stored!!.latLng.latitude, 0.0001)
        assertEquals("Updated longitude should be reflected", newPosition.longitude, stored.latLng.longitude, 0.0001)
    }

    @Test
    fun circle_updatingRadius_reflectsInAnnotation() = withApplier { applier ->
        val manager = applier.getOrCreateCircleManagerForLayerId("circles", null, null)
        val circle = manager.create(CircleOptions().withLatLng(LatLng(48.0, 7.0)).withCircleRadius(5f))

        circle.circleRadius = 20f
        manager.update(circle)

        val stored = manager.annotations.get(circle.id)
        assertEquals("Updated radius should be reflected", 20f, stored!!.circleRadius, 0.01f)
    }

    @Test
    fun polyline_updatingPoints_reflectsInAnnotation() = withApplier { applier ->
        val manager = applier.getOrCreateLineManagerForLayerId("lines", null, null)
        val original = listOf(LatLng(48.0, 7.0), LatLng(49.0, 8.0))
        val line = manager.create(
            LineOptions().withLatLngs(original).withLineColor("#FF0000").withLineWidth(2f)
        )

        val updated = listOf(LatLng(51.5, -0.12), LatLng(52.0, 0.0), LatLng(52.5, 1.0))
        line.latLngs = updated
        manager.update(line)

        val stored = manager.annotations.get(line.id)
        assertEquals("Updated point count should be reflected", 3, stored!!.latLngs.size)
    }
}
