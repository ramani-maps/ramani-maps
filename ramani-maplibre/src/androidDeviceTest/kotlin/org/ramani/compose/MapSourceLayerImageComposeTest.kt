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
import androidx.compose.runtime.mutableIntStateOf
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
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.Source
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MapSourceLayerImageComposeTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ComposeTestActivity::class.java)

    private val blankStyleJson = """{"version": 8, "sources": {}, "layers": []}"""

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
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = {
                        loadedStyle = it
                        styleLatch.countDown()
                    },
                ) {
                    MapSource { GeoJsonSource("compose-source") }
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
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = {
                        loadedStyle = it
                        styleLatch.countDown()
                    },
                ) {
                    MapLayer { BackgroundLayer("compose-layer") }
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
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = {
                        loadedStyle = it
                        styleLatch.countDown()
                    },
                ) {
                    MapSource { GeoJsonSource("combined-source") }
                    MapLayer { BackgroundLayer("combined-layer") }
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
                        MapSource { GeoJsonSource("conditional-source") }
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

    @Test
    fun mapSource_updateLambda_calledOnRecomposition() {
        val styleLatch = CountDownLatch(1)
        val updateLatch = CountDownLatch(1)
        val triggerRecomposition = mutableIntStateOf(0)
        var updatedSource: Source? = null

        activityRule.scenario.onActivity { activity ->
            activity.setContent {
                // Read the state to trigger recomposition when it changes
                @Suppress("UNUSED_VARIABLE")
                val trigger = triggerRecomposition.intValue

                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = { styleLatch.countDown() },
                ) {
                    MapSource(
                        update = { source ->
                            updatedSource = source
                            updateLatch.countDown()
                        },
                    ) {
                        GeoJsonSource("reactive-source")
                    }
                }
            }
        }

        assertTrue("Style should load within 15s", styleLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(2000)

        // Trigger a recomposition to invoke the update lambda
        activityRule.scenario.onActivity {
            triggerRecomposition.intValue = 1
        }

        assertTrue("Update lambda should be called within 5s", updateLatch.await(5, TimeUnit.SECONDS))
        assertNotNull("Update lambda should receive the source", updatedSource)
    }

    @Test
    fun mapLayer_updateLambda_calledOnRecomposition() {
        val styleLatch = CountDownLatch(1)
        val updateLatch = CountDownLatch(1)
        val triggerRecomposition = mutableIntStateOf(0)
        var updatedLayer: Layer? = null

        activityRule.scenario.onActivity { activity ->
            activity.setContent {
                @Suppress("UNUSED_VARIABLE")
                val trigger = triggerRecomposition.intValue

                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = { styleLatch.countDown() },
                ) {
                    MapLayer(
                        update = { layer ->
                            updatedLayer = layer
                            updateLatch.countDown()
                        },
                    ) {
                        BackgroundLayer("reactive-layer")
                    }
                }
            }
        }

        assertTrue("Style should load within 15s", styleLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(2000)

        activityRule.scenario.onActivity {
            triggerRecomposition.intValue = 1
        }

        assertTrue("Update lambda should be called within 5s", updateLatch.await(5, TimeUnit.SECONDS))
        assertNotNull("Update lambda should receive the layer", updatedLayer)
    }

    @Test
    fun mapSource_updateLambda_canChangeAtRuntime() {
        val styleLatch = CountDownLatch(1)
        val secondUpdateLatch = CountDownLatch(1)
        var firstUpdateCalled = false
        var secondUpdateCalled = false
        val useSecondUpdate = mutableStateOf(false)

        activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val currentUpdate: (Source) -> Unit = if (useSecondUpdate.value) {
                    { secondUpdateCalled = true; secondUpdateLatch.countDown() }
                } else {
                    { firstUpdateCalled = true }
                }

                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = { styleLatch.countDown() },
                ) {
                    MapSource(update = currentUpdate) {
                        GeoJsonSource("swap-update-source")
                    }
                }
            }
        }

        assertTrue("Style should load within 15s", styleLatch.await(15, TimeUnit.SECONDS))
        Thread.sleep(2000)

        // Switch to the second update lambda
        activityRule.scenario.onActivity {
            useSecondUpdate.value = true
        }

        assertTrue("Second update should be called within 5s", secondUpdateLatch.await(5, TimeUnit.SECONDS))
        assertTrue("Second update lambda should have been called", secondUpdateCalled)
    }
}
