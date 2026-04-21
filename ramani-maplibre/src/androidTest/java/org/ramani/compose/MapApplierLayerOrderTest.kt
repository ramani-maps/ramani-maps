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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.Style
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MapApplierLayerOrderTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(TestActivity::class.java)

    // A minimal valid MapLibre style with no base layers or sources.
    private val blankStyle = """{"version": 8, "sources": {}, "layers": []}"""

    private fun withApplier(block: (MapApplier, Style) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        activityRule.scenario.onActivity { activity ->
            activity.mapView.getMapAsync { map ->
                map.setStyle(Style.Builder().fromJson(blankStyle)) { style ->
                    try {
                        val applier = MapApplier(map, activity.mapView, mutableStateOf(style))
                        block(applier, style)
                    } catch (t: Throwable) {
                        error = t
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }

        assertTrue("Test timed out waiting for map style", latch.await(15, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    // Returns the index of the layer with the given ID in style.layers (bottom=0).
    private fun Style.layerIndex(layerId: String): Int =
        layers.indexOfFirst { it.id == layerId }

    @Test
    fun backwardReference_isPlacedCorrectlyAtCreationTime() = withApplier { applier, style ->
        // "route" is declared first, then "dot" says aboveLayerId = "route".
        // Since "route" already exists when "dot" is created, no fixup is needed.
        val routeManager = applier.getOrCreateLineManagerForLayerId("route", null, null)
        val dotManager = applier.getOrCreateCircleManagerForLayerId("dot", aboveLayerId = "route", belowLayerId = null)

        applier.onEndChanges()

        val routeIndex = style.layerIndex(routeManager.layerId)
        val dotIndex = style.layerIndex(dotManager.layerId)

        assertTrue("route index ($routeIndex) should be valid", routeIndex >= 0)
        assertTrue("dot index ($dotIndex) should be valid", dotIndex >= 0)
        assertTrue("dot should be above route", dotIndex > routeIndex)
    }

    @Test
    fun forwardReference_isFixedUpInOnEndChanges() = withApplier { applier, style ->
        // "dot" is declared first with aboveLayerId = "route", but "route" doesn't exist yet.
        // After "route" is declared and onEndChanges() is called, dot should end up above route.
        val dotManager = applier.getOrCreateCircleManagerForLayerId("dot", aboveLayerId = "route", belowLayerId = null)
        val routeManager = applier.getOrCreateLineManagerForLayerId("route", null, null)

        applier.onEndChanges()

        val routeIndex = style.layerIndex(routeManager.layerId)
        val dotIndex = style.layerIndex(dotManager.layerId)

        assertTrue("route index ($routeIndex) should be valid", routeIndex >= 0)
        assertTrue("dot index ($dotIndex) should be valid", dotIndex >= 0)
        assertTrue("dot should be above route (forward ref fixed up)", dotIndex > routeIndex)
    }

    @Test
    fun belowLayerId_placesLayerBelow() = withApplier { applier, style ->
        val symbolsManager = applier.getOrCreateSymbolManagerForLayerId("labels", null, null)
        val fillManager = applier.getOrCreateFillManagerForLayerId("zones", aboveLayerId = null, belowLayerId = "labels")

        applier.onEndChanges()

        val labelsIndex = style.layerIndex(symbolsManager.layerId)
        val zonesIndex = style.layerIndex(fillManager.layerId)

        assertTrue("labels index ($labelsIndex) should be valid", labelsIndex >= 0)
        assertTrue("zones index ($zonesIndex) should be valid", zonesIndex >= 0)
        assertTrue("zones should be below labels", zonesIndex < labelsIndex)
    }

    @Test
    fun sameLayerId_returnsSameManagerInstance() = withApplier { applier, _ ->
        val manager1 = applier.getOrCreateCircleManagerForLayerId("dots", null, null)
        val manager2 = applier.getOrCreateCircleManagerForLayerId("dots", null, null)

        assertSame("Same layerId should return the same CircleManager instance", manager1, manager2)
    }

    @Test
    fun missingReference_doesNotCrash() = withApplier { applier, style ->
        val dotManager = applier.getOrCreateCircleManagerForLayerId(
            "dot", aboveLayerId = "nonexistent", belowLayerId = null
        )

        // onEndChanges() should silently skip the unresolvable ordering
        applier.onEndChanges()

        // Layer should still exist in the style (just at default position)
        assertNotNull("dot layer should still exist", style.getLayer(dotManager.layerId))
    }

    @Test
    fun zIndex_backwardCompatibility() = withApplier { applier, style ->
        // Old zIndex API must continue to work unchanged.
        val low = applier.getOrCreateCircleManagerForZIndex(1)
        val high = applier.getOrCreateCircleManagerForZIndex(5)

        val lowIndex = style.layerIndex(low.layerId)
        val highIndex = style.layerIndex(high.layerId)

        assertTrue("zIndex=5 layer should be above zIndex=1 layer", highIndex > lowIndex)
    }

    @Test
    fun multipleLayerTypes_canReferenceEachOther() = withApplier { applier, style ->
        val fillManager = applier.getOrCreateFillManagerForLayerId("area", null, null)
        val lineManager = applier.getOrCreateLineManagerForLayerId("border", aboveLayerId = "area", belowLayerId = null)
        val circleManager = applier.getOrCreateCircleManagerForLayerId("vertex", aboveLayerId = "border", belowLayerId = null)

        applier.onEndChanges()

        val areaIndex = style.layerIndex(fillManager.layerId)
        val borderIndex = style.layerIndex(lineManager.layerId)
        val vertexIndex = style.layerIndex(circleManager.layerId)

        assertTrue("area ($areaIndex), border ($borderIndex), vertex ($vertexIndex) should all be valid",
            areaIndex >= 0 && borderIndex >= 0 && vertexIndex >= 0)
        assertTrue("border should be above area", borderIndex > areaIndex)
        assertTrue("vertex should be above border", vertexIndex > borderIndex)
    }
}
