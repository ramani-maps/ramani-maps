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

    @Test
    fun transitiveForwardReferences_allLayersDeclaredInReverseOrder() = withApplier { applier, style ->
        // All declared in reverse dependency order — C above B, B above A, but A declared last.
        val cManager = applier.getOrCreateCircleManagerForLayerId("C", aboveLayerId = "B", belowLayerId = null)
        val bManager = applier.getOrCreateLineManagerForLayerId("B", aboveLayerId = "A", belowLayerId = null)
        val aManager = applier.getOrCreateFillManagerForLayerId("A", null, null)

        applier.onEndChanges()

        val aIndex = style.layerIndex(aManager.layerId)
        val bIndex = style.layerIndex(bManager.layerId)
        val cIndex = style.layerIndex(cManager.layerId)

        assertTrue("A < B < C: A=$aIndex, B=$bIndex, C=$cIndex", aIndex < bIndex && bIndex < cIndex)
    }

    @Test
    fun onEndChanges_idempotent_callingTwiceDoesNotCorruptOrdering() = withApplier { applier, style ->
        val routeManager = applier.getOrCreateLineManagerForLayerId("route", null, null)
        val dotManager = applier.getOrCreateCircleManagerForLayerId("dot", aboveLayerId = "route", belowLayerId = null)

        // Forward ref case — second call should be a no-op (pendingOrders cleared after first call)
        applier.onEndChanges()
        applier.onEndChanges()

        val routeIndex = style.layerIndex(routeManager.layerId)
        val dotIndex = style.layerIndex(dotManager.layerId)

        assertTrue("dot should still be above route after double onEndChanges", dotIndex > routeIndex)
    }

    @Test
    fun backwardRef_whenTargetHasPendingOrder_followsTargetAfterReorder() = withApplier { applier, style ->
        // Reproduces the bug: circle has a forward-ref to polyline (goes to pending orders),
        // symbol has a backward-ref to circle (circle already in registry).
        // Before the fix, symbol was placed immediately above circle's original position,
        // then circle was moved above polyline — leaving symbol stranded below polyline.
        val circleManager = applier.getOrCreateCircleManagerForLayerId("circle", aboveLayerId = "polyline", belowLayerId = null)
        val symbolManager = applier.getOrCreateSymbolManagerForLayerId("symbol", aboveLayerId = "circle", belowLayerId = null)
        val lineManager = applier.getOrCreateLineManagerForLayerId("polyline", null, null)

        applier.onEndChanges()

        val polylineIndex = style.layerIndex(lineManager.layerId)
        val circleIndex = style.layerIndex(circleManager.layerId)
        val symbolIndex = style.layerIndex(symbolManager.layerId)

        assertTrue("polyline ($polylineIndex), circle ($circleIndex), symbol ($symbolIndex) should all be valid",
            polylineIndex >= 0 && circleIndex >= 0 && symbolIndex >= 0)
        assertTrue("circle should be above polyline", circleIndex > polylineIndex)
        assertTrue("symbol should be above circle", symbolIndex > circleIndex)
    }

    @Test
    fun siblings_declarationOrderIsPreserved() = withApplier { applier, style ->
        // Both polyline and symbol declare aboveLayerId = "circle".
        // Expected final order (bottom to top): circle -> polyline -> symbol (declaration order).
        val circleManager = applier.getOrCreateCircleManagerForLayerId("circle", null, null)
        val lineManager = applier.getOrCreateLineManagerForLayerId("polyline", aboveLayerId = "circle", belowLayerId = null)
        val symbolManager = applier.getOrCreateSymbolManagerForLayerId("symbol", aboveLayerId = "circle", belowLayerId = null)

        applier.onEndChanges()

        val circleIndex = style.layerIndex(circleManager.layerId)
        val polylineIndex = style.layerIndex(lineManager.layerId)
        val symbolIndex = style.layerIndex(symbolManager.layerId)

        assertTrue("all layers should be valid", circleIndex >= 0 && polylineIndex >= 0 && symbolIndex >= 0)
        assertTrue("polyline should be above circle", polylineIndex > circleIndex)
        assertTrue("symbol should be above polyline", symbolIndex > polylineIndex)
    }

    @Test
    fun siblings_belowLayerId_declarationOrderIsPreserved() = withApplier { applier, style ->
        // Both polyline and symbol declare belowLayerId = "circle".
        // Expected final order (bottom to top): polyline -> symbol -> circle (declaration order).
        val circleManager = applier.getOrCreateCircleManagerForLayerId("circle", null, null)
        val lineManager = applier.getOrCreateLineManagerForLayerId("polyline", aboveLayerId = null, belowLayerId = "circle")
        val symbolManager = applier.getOrCreateSymbolManagerForLayerId("symbol", aboveLayerId = null, belowLayerId = "circle")

        applier.onEndChanges()

        val circleIndex = style.layerIndex(circleManager.layerId)
        val polylineIndex = style.layerIndex(lineManager.layerId)
        val symbolIndex = style.layerIndex(symbolManager.layerId)

        assertTrue("all layers should be valid", circleIndex >= 0 && polylineIndex >= 0 && symbolIndex >= 0)
        assertTrue("polyline should be below symbol", polylineIndex < symbolIndex)
        assertTrue("symbol should be below circle", symbolIndex < circleIndex)
    }

    @Test
    fun mixed_aboveAndBelow_differentAnchors_bothBetweenAnchors() = withApplier { applier, style ->
        // symbol says aboveLayerId = "circle1", polyline says belowLayerId = "circle2".
        // "above circle1" anchors symbol just above circle1 (bottom of gap).
        // "below circle2" anchors polyline just below circle2 (top of gap).
        // Result: circle1 -> symbol -> polyline -> circle2, regardless of declaration order.
        val circle1 = applier.getOrCreateCircleManagerForLayerId("circle1", null, null)
        val symbolManager = applier.getOrCreateSymbolManagerForLayerId("symbol", aboveLayerId = "circle1", belowLayerId = null)
        val lineManager = applier.getOrCreateLineManagerForLayerId("polyline", aboveLayerId = null, belowLayerId = "circle2")
        val circle2 = applier.getOrCreateCircleManagerForLayerId("circle2", null, null)

        applier.onEndChanges()

        val c1 = style.layerIndex(circle1.layerId)
        val sym = style.layerIndex(symbolManager.layerId)
        val line = style.layerIndex(lineManager.layerId)
        val c2 = style.layerIndex(circle2.layerId)

        assertTrue("all layers should be valid", c1 >= 0 && sym >= 0 && line >= 0 && c2 >= 0)
        assertTrue("symbol should be above circle1", sym > c1)
        assertTrue("symbol should be below circle2", sym < c2)
        assertTrue("polyline should be above circle1", line > c1)
        assertTrue("polyline should be below circle2", line < c2)
    }

    @Test
    fun mixed_aboveAndBelow_reversedDeclaration_bothBetweenAnchors() = withApplier { applier, style ->
        // Same constraints as above but polyline declared before symbol.
        // Result should still have both between circle1 and circle2.
        val circle1 = applier.getOrCreateCircleManagerForLayerId("circle1", null, null)
        val lineManager = applier.getOrCreateLineManagerForLayerId("polyline", aboveLayerId = null, belowLayerId = "circle2")
        val symbolManager = applier.getOrCreateSymbolManagerForLayerId("symbol", aboveLayerId = "circle1", belowLayerId = null)
        val circle2 = applier.getOrCreateCircleManagerForLayerId("circle2", null, null)

        applier.onEndChanges()

        val c1 = style.layerIndex(circle1.layerId)
        val sym = style.layerIndex(symbolManager.layerId)
        val line = style.layerIndex(lineManager.layerId)
        val c2 = style.layerIndex(circle2.layerId)

        assertTrue("all layers should be valid", c1 >= 0 && sym >= 0 && line >= 0 && c2 >= 0)
        assertTrue("symbol should be above circle1", sym > c1)
        assertTrue("symbol should be below circle2", sym < c2)
        assertTrue("polyline should be above circle1", line > c1)
        assertTrue("polyline should be below circle2", line < c2)
    }

    @Test
    fun sametype_circleLayersCanChain() = withApplier { applier, style ->
        val baseCircles = applier.getOrCreateCircleManagerForLayerId("base-circles", null, null)
        val topCircles = applier.getOrCreateCircleManagerForLayerId("top-circles", aboveLayerId = "base-circles", belowLayerId = null)

        applier.onEndChanges()

        val baseIndex = style.layerIndex(baseCircles.layerId)
        val topIndex = style.layerIndex(topCircles.layerId)

        assertTrue("top-circles should be above base-circles", topIndex > baseIndex)
    }
}
