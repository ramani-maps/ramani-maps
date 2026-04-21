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
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.Style
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Verifies that the zIndex path and the named-layer path in MapApplier are independent,
 * and that manager caching within each path works correctly.
 */
@RunWith(AndroidJUnit4::class)
class MapApplierIsolationTest {

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

    // --- Manager caching: same call returns same instance ---

    @Test
    fun zIndex_sameIndexReturnsSameCircleManager() = withApplier { applier ->
        val a = applier.getOrCreateCircleManagerForZIndex(3)
        val b = applier.getOrCreateCircleManagerForZIndex(3)
        assertSame("Same zIndex should return the same CircleManager", a, b)
    }

    @Test
    fun namedLayer_sameIdReturnsSameCircleManager() = withApplier { applier ->
        val a = applier.getOrCreateCircleManagerForLayerId("dots", null, null)
        val b = applier.getOrCreateCircleManagerForLayerId("dots", null, null)
        assertSame("Same layerId should return the same CircleManager", a, b)
    }

    @Test
    fun namedLayer_sameIdReturnsSameSymbolManager() = withApplier { applier ->
        val a = applier.getOrCreateSymbolManagerForLayerId("labels", null, null)
        val b = applier.getOrCreateSymbolManagerForLayerId("labels", null, null)
        assertSame("Same layerId should return the same SymbolManager", a, b)
    }

    @Test
    fun namedLayer_sameIdReturnsSameLineManager() = withApplier { applier ->
        val a = applier.getOrCreateLineManagerForLayerId("routes", null, null)
        val b = applier.getOrCreateLineManagerForLayerId("routes", null, null)
        assertSame("Same layerId should return the same LineManager", a, b)
    }

    @Test
    fun namedLayer_sameIdReturnsSameFillManager() = withApplier { applier ->
        val a = applier.getOrCreateFillManagerForLayerId("areas", null, null)
        val b = applier.getOrCreateFillManagerForLayerId("areas", null, null)
        assertSame("Same layerId should return the same FillManager", a, b)
    }

    // --- zIndex and named-layer paths are isolated ---

    @Test
    fun zIndexAndNamedLayer_zeroAndNamedAreDistinctCircleManagers() = withApplier { applier ->
        val byZIndex = applier.getOrCreateCircleManagerForZIndex(0)
        val byName = applier.getOrCreateCircleManagerForLayerId("circles", null, null)
        assertNotSame("zIndex=0 and layerId='circles' must be different managers", byZIndex, byName)
    }

    @Test
    fun zIndexAndNamedLayer_doNotShareSymbolManagers() = withApplier { applier ->
        val byZIndex = applier.getOrCreateSymbolManagerForZIndex(0)
        val byName = applier.getOrCreateSymbolManagerForLayerId("symbols", null, null)
        assertNotSame("zIndex and named-layer symbol managers must be distinct", byZIndex, byName)
    }

    // --- Different IDs produce different managers ---

    @Test
    fun differentNamedLayerIds_produceDifferentCircleManagers() = withApplier { applier ->
        val a = applier.getOrCreateCircleManagerForLayerId("alpha", null, null)
        val b = applier.getOrCreateCircleManagerForLayerId("beta", null, null)
        assertNotSame("Different layerIds should produce different CircleManagers", a, b)
    }

    @Test
    fun differentZIndexes_produceDifferentCircleManagers() = withApplier { applier ->
        val low = applier.getOrCreateCircleManagerForZIndex(1)
        val high = applier.getOrCreateCircleManagerForZIndex(5)
        assertNotSame("Different zIndexes should produce different CircleManagers", low, high)
    }

    // --- Mixed usage: zIndex ordering unaffected by named layers ---

    @Test
    fun zIndexOrdering_unaffectedByNamedLayerCreation() = withApplier { applier ->
        // Create a named layer between the two zIndex layers
        val low = applier.getOrCreateCircleManagerForZIndex(1)
        applier.getOrCreateLineManagerForLayerId("named", null, null)
        val high = applier.getOrCreateCircleManagerForZIndex(5)

        // zIndex ordering should still hold
        val style = applier.style.value!!
        val lowIdx = style.layers.indexOfFirst { it.id == low.layerId }
        val highIdx = style.layers.indexOfFirst { it.id == high.layerId }

        assertTrue("zIndex=5 should be above zIndex=1 regardless of named layer creation",
            highIdx > lowIdx)
    }
}
