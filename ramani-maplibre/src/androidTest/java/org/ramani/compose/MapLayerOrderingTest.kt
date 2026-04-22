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

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.BackgroundLayer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MapLayerOrderingTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ComposeTestActivity::class.java)

    private val blankStyleJson = """{"version": 8, "sources": {}, "layers": []}"""
    private val altStyleJson = """{"version": 8, "sources": {}, "layers": [], "name": "alt"}"""

    private fun runOnUiThread(block: () -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        activityRule.scenario.onActivity {
            try {
                block()
            } catch (t: Throwable) {
                error = t
            } finally {
                latch.countDown()
            }
        }

        assertTrue("UI thread block timed out", latch.await(15, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    /**
     * Returns the index of a layer whose ID contains the given substring.
     * Annotation manager layers have auto-generated IDs, so we search by substring.
     * Bottom of the stack = index 0.
     */
    private fun Style.layerIndexContaining(substring: String): Int =
        layers.indexOfFirst { it.id.contains(substring) }

    @Test
    fun annotationLayer_isAboveMapLayer_onInitialLoad() {
        val styleLatch = CountDownLatch(1)
        var loadedStyle: Style? = null

        activityRule.scenario.onActivity { activity ->
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = {
                        loadedStyle = it
                        styleLatch.countDown()
                    },
                ) {
                    MapLayer { BackgroundLayer("custom-bg") }
                    Circle(
                        center = LatLng(0.0, 0.0),
                        radius = 10F,
                        isDraggable = false,
                    )
                }
            }
        }

        assertTrue("Style should load within 15s", styleLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(3000)

        runOnUiThread {
            val style = loadedStyle!!
            val layerIds = style.layers.map { it.id }
            val bgIndex = layerIds.indexOf("custom-bg")

            assertTrue("custom-bg layer should exist (index=$bgIndex)", bgIndex >= 0)
            assertTrue(
                "There should be at least one layer (the annotation) above custom-bg, " +
                    "but custom-bg is at index $bgIndex of ${layerIds.size} layers: $layerIds",
                bgIndex < layerIds.size - 1
            )
        }
    }

    @Test
    fun annotationLayer_remainsAboveMapLayer_afterStyleSwap() {
        val styleLoadCount = java.util.concurrent.atomic.AtomicInteger(0)
        val secondStyleLatch = CountDownLatch(1)
        val currentStyle = mutableStateOf<MapStyle>(MapStyle.Json(blankStyleJson))
        var loadedStyle: Style? = null

        activityRule.scenario.onActivity { activity ->
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = currentStyle.value,
                    onStyleLoaded = {
                        loadedStyle = it
                        val count = styleLoadCount.incrementAndGet()
                        if (count == 2) secondStyleLatch.countDown()
                    },
                ) {
                    MapLayer { BackgroundLayer("custom-bg") }
                    Circle(
                        center = LatLng(0.0, 0.0),
                        radius = 10F,
                        isDraggable = false,
                    )
                }
            }
        }

        // Wait for initial style load
        val initialLatch = CountDownLatch(1)
        Thread {
            while (styleLoadCount.get() < 1) Thread.sleep(100)
            initialLatch.countDown()
        }.start()
        assertTrue("Initial style should load", initialLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(2000)

        // Swap the style
        activityRule.scenario.onActivity {
            currentStyle.value = MapStyle.Json(altStyleJson)
        }

        assertTrue("Second style should load", secondStyleLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(3000)

        runOnUiThread {
            val style = loadedStyle!!
            val layerIds = style.layers.map { it.id }
            val bgIndex = layerIds.indexOf("custom-bg")

            assertTrue("custom-bg layer should exist after style swap (index=$bgIndex)", bgIndex >= 0)
            assertTrue(
                "There should be at least one layer (the annotation) above custom-bg after style swap, " +
                    "but custom-bg is at index $bgIndex of ${layerIds.size} layers: $layerIds",
                bgIndex < layerIds.size - 1
            )
        }
    }

    @Test
    fun mapLayerAndSource_survivesStyleSwap() {
        val styleLoadCount = java.util.concurrent.atomic.AtomicInteger(0)
        val secondStyleLatch = CountDownLatch(1)
        val currentStyle = mutableStateOf<MapStyle>(MapStyle.Json(blankStyleJson))
        var loadedStyle: Style? = null

        activityRule.scenario.onActivity { activity ->
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = currentStyle.value,
                    onStyleLoaded = {
                        loadedStyle = it
                        val count = styleLoadCount.incrementAndGet()
                        if (count == 2) secondStyleLatch.countDown()
                    },
                ) {
                    MapLayer { BackgroundLayer("surviving-layer") }
                }
            }
        }

        // Wait for initial style load
        val initialLatch = CountDownLatch(1)
        Thread {
            while (styleLoadCount.get() < 1) Thread.sleep(100)
            initialLatch.countDown()
        }.start()
        assertTrue("Initial style should load", initialLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(2000)

        // Verify layer exists before swap
        runOnUiThread {
            assertNotNull("Layer should exist before swap", loadedStyle?.getLayer("surviving-layer"))
        }

        // Swap the style
        activityRule.scenario.onActivity {
            currentStyle.value = MapStyle.Json(altStyleJson)
        }

        assertTrue("Second style should load", secondStyleLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(3000)

        runOnUiThread {
            assertNotNull(
                "Layer should still exist after style swap",
                loadedStyle?.getLayer("surviving-layer")
            )
        }
    }

    @Test
    fun multipleMapLayers_preserveDeclarationOrder() {
        val styleLatch = CountDownLatch(1)
        var loadedStyle: Style? = null

        activityRule.scenario.onActivity { activity ->
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = {
                        loadedStyle = it
                        styleLatch.countDown()
                    },
                ) {
                    MapLayer { BackgroundLayer("layer-a") }
                    MapLayer { BackgroundLayer("layer-b") }
                    MapLayer { BackgroundLayer("layer-c") }
                }
            }
        }

        assertTrue("Style should load within 15s", styleLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(3000)

        runOnUiThread {
            val style = loadedStyle!!
            val aIndex = style.layers.indexOfFirst { it.id == "layer-a" }
            val bIndex = style.layers.indexOfFirst { it.id == "layer-b" }
            val cIndex = style.layers.indexOfFirst { it.id == "layer-c" }

            assertTrue("All layers should exist", aIndex >= 0 && bIndex >= 0 && cIndex >= 0)
            assertTrue(
                "Layers should follow declaration order: a=$aIndex < b=$bIndex < c=$cIndex",
                aIndex < bIndex && bIndex < cIndex
            )
        }
    }
}
