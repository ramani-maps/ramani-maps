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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class SourceLayerImageNodeTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(TestActivity::class.java)

    private val blankStyle = """{"version": 8, "sources": {}, "layers": []}"""

    private fun withStyle(block: (Style) -> Unit) {
        val latch = CountDownLatch(1)
        var error: Throwable? = null

        activityRule.scenario.onActivity { activity ->
            activity.mapView.getMapAsync { map ->
                map.setStyle(Style.Builder().fromJson(blankStyle)) { style ->
                    try {
                        block(style)
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

    // Runs a block on the UI thread (needed for MapLibre native objects)
    private fun onUiThread(block: () -> Unit) {
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

        assertTrue("Test timed out", latch.await(15, TimeUnit.SECONDS))
        error?.let { throw it }
    }

    // --- SourceNode ---

    @Test
    fun sourceNode_onAttached_addsSourceToStyle() = withStyle { style ->
        val source = GeoJsonSource("test-source")
        val node = SourceNode(mutableStateOf(style), source)

        node.onAttached()

        assertNotNull("Source should exist in style after onAttached", style.getSource("test-source"))
    }

    @Test
    fun sourceNode_onRemoved_removesSourceFromStyle() = withStyle { style ->
        val source = GeoJsonSource("test-source")
        val node = SourceNode(mutableStateOf(style), source)
        node.onAttached()

        node.onRemoved()

        assertNull("Source should not exist in style after onRemoved", style.getSource("test-source"))
    }

    @Test
    fun sourceNode_onCleared_removesSourceFromStyle() = withStyle { style ->
        val source = GeoJsonSource("test-source")
        val node = SourceNode(mutableStateOf(style), source)
        node.onAttached()

        node.onCleared()

        assertNull("Source should not exist in style after onCleared", style.getSource("test-source"))
    }

    @Test
    fun sourceNode_nullStyle_doesNotCrash() = onUiThread {
        val source = GeoJsonSource("test-source")
        val node = SourceNode(mutableStateOf(null), source)

        node.onAttached()
        node.onRemoved()
        node.onCleared()
    }

    // --- LayerNode ---

    @Test
    fun layerNode_onAttached_addsLayerToStyle() = withStyle { style ->
        val layer = BackgroundLayer("test-layer")
        val node = LayerNode(mutableStateOf(style), layer)

        node.onAttached()

        assertNotNull("Layer should exist in style after onAttached", style.getLayer("test-layer"))
    }

    @Test
    fun layerNode_onRemoved_removesLayerFromStyle() = withStyle { style ->
        val layer = BackgroundLayer("test-layer")
        val node = LayerNode(mutableStateOf(style), layer)
        node.onAttached()

        node.onRemoved()

        assertNull("Layer should not exist in style after onRemoved", style.getLayer("test-layer"))
    }

    @Test
    fun layerNode_onCleared_removesLayerFromStyle() = withStyle { style ->
        val layer = BackgroundLayer("test-layer")
        val node = LayerNode(mutableStateOf(style), layer)
        node.onAttached()

        node.onCleared()

        assertNull("Layer should not exist in style after onCleared", style.getLayer("test-layer"))
    }

    @Test
    fun layerNode_nullStyle_doesNotCrash() = onUiThread {
        val layer = BackgroundLayer("test-layer")
        val node = LayerNode(mutableStateOf(null), layer)

        node.onAttached()
        node.onRemoved()
        node.onCleared()
    }

    // --- ImageNode ---

    @Test
    fun imageNode_onAttached_addsImageToStyle() = withStyle { style ->
        val context = activityRule.scenario.let { scenario ->
            var ctx: android.content.Context? = null
            scenario.onActivity { ctx = it }
            ctx!!
        }
        val node = ImageNode(
            mutableStateOf(style), context,
            "test-image", org.maplibre.android.R.drawable.maplibre_marker_icon_default
        )

        node.onAttached()

        assertNotNull("Image should exist in style after onAttached", style.getImage("test-image"))
    }

    @Test
    fun imageNode_onRemoved_removesImageFromStyle() = withStyle { style ->
        val context = activityRule.scenario.let { scenario ->
            var ctx: android.content.Context? = null
            scenario.onActivity { ctx = it }
            ctx!!
        }
        val node = ImageNode(
            mutableStateOf(style), context,
            "test-image", org.maplibre.android.R.drawable.maplibre_marker_icon_default
        )
        node.onAttached()

        node.onRemoved()

        assertNull("Image should not exist in style after onRemoved", style.getImage("test-image"))
    }

    @Test
    fun imageNode_onCleared_removesImageFromStyle() = withStyle { style ->
        val context = activityRule.scenario.let { scenario ->
            var ctx: android.content.Context? = null
            scenario.onActivity { ctx = it }
            ctx!!
        }
        val node = ImageNode(
            mutableStateOf(style), context,
            "test-image", org.maplibre.android.R.drawable.maplibre_marker_icon_default
        )
        node.onAttached()

        node.onCleared()

        assertNull("Image should not exist in style after onCleared", style.getImage("test-image"))
    }

    @Test
    fun imageNode_nullStyle_doesNotCrash() = onUiThread {
        val context = activityRule.scenario.let { scenario ->
            var ctx: android.content.Context? = null
            scenario.onActivity { ctx = it }
            ctx!!
        }
        val node = ImageNode(
            mutableStateOf(null), context,
            "test-image", org.maplibre.android.R.drawable.maplibre_marker_icon_default
        )

        node.onAttached()
        node.onRemoved()
        node.onCleared()
    }
}
