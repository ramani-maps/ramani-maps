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
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.sources.GeoJsonSource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MapSourceLayerImageComposeTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ComposeTestActivity::class.java)

    private val blankStyleJson = """{"version": 8, "sources": {}, "layers": []}"""

    // Runs a block on the UI thread and waits for it to complete
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

    @Test
    fun mapSource_addsSourceToStyle_throughCompose() {
        val styleLatch = CountDownLatch(1)
        var loadedStyle: Style? = null

        activityRule.scenario.onActivity { activity ->
            val source = GeoJsonSource("compose-source")
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = {
                        loadedStyle = it
                        styleLatch.countDown()
                    },
                ) {
                    MapSource(source = source)
                }
            }
        }

        assertTrue("Style should load within 15s", styleLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(2000)

        runOnUiThread {
            assertNotNull("Source should be present in style", loadedStyle?.getSource("compose-source"))
        }
    }

    @Test
    fun mapLayer_addsLayerToStyle_throughCompose() {
        val styleLatch = CountDownLatch(1)
        var loadedStyle: Style? = null

        activityRule.scenario.onActivity { activity ->
            val layer = BackgroundLayer("compose-layer")
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = {
                        loadedStyle = it
                        styleLatch.countDown()
                    },
                ) {
                    MapLayer(layer = layer)
                }
            }
        }

        assertTrue("Style should load within 15s", styleLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(2000)

        runOnUiThread {
            assertNotNull("Layer should be present in style", loadedStyle?.getLayer("compose-layer"))
        }
    }

    @Test
    fun mapSourceAndLayer_workTogether_throughCompose() {
        val styleLatch = CountDownLatch(1)
        var loadedStyle: Style? = null

        activityRule.scenario.onActivity { activity ->
            val source = GeoJsonSource("combined-source")
            val layer = BackgroundLayer("combined-layer")
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = {
                        loadedStyle = it
                        styleLatch.countDown()
                    },
                ) {
                    MapSource(source = source)
                    MapLayer(layer = layer)
                }
            }
        }

        assertTrue("Style should load within 15s", styleLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(2000)

        runOnUiThread {
            assertNotNull("Source should be present in style", loadedStyle?.getSource("combined-source"))
            assertNotNull("Layer should be present in style", loadedStyle?.getLayer("combined-layer"))
        }
    }

    @Test
    fun mapSource_conditionallyAdded_appearsInStyle() {
        val showSource = mutableStateOf(false)
        val styleLatch = CountDownLatch(1)
        var loadedStyle: Style? = null

        activityRule.scenario.onActivity { activity ->
            val source = GeoJsonSource("conditional-source")
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = {
                        loadedStyle = it
                        styleLatch.countDown()
                    },
                ) {
                    if (showSource.value) {
                        MapSource(source = source)
                    }
                }
            }
        }

        assertTrue("Style should load within 15s", styleLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(2000)

        // Toggle the source on
        activityRule.scenario.onActivity {
            showSource.value = true
        }
        Thread.sleep(2000)

        runOnUiThread {
            assertNotNull(
                "Source should appear after conditional toggle",
                loadedStyle?.getSource("conditional-source")
            )
        }
    }
}
