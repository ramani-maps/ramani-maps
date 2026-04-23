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
    fun sourceNode_attach_addsSourceToStyle() = withStyle { style ->
        val node = SourceNode(mutableStateOf(style)) { GeoJsonSource("test-source") }

        node.attach()

        assertNotNull("Source should exist in style after attach", style.getSource("test-source"))
    }

    @Test
    fun sourceNode_onRemoved_removesSourceFromStyle() = withStyle { style ->
        val node = SourceNode(mutableStateOf(style)) { GeoJsonSource("test-source") }
        node.attach()

        node.onRemoved()

        assertNull("Source should not exist in style after onRemoved", style.getSource("test-source"))
    }

    @Test
    fun sourceNode_onCleared_removesSourceFromStyle() = withStyle { style ->
        val node = SourceNode(mutableStateOf(style)) { GeoJsonSource("test-source") }
        node.attach()

        node.onCleared()

        assertNull("Source should not exist in style after onCleared", style.getSource("test-source"))
    }

    @Test
    fun sourceNode_reattach_recreatesSource() = withStyle { style ->
        val node = SourceNode(mutableStateOf(style)) { GeoJsonSource("test-source") }
        node.attach()
        val firstSource = node.source

        node.onRemoved()
        node.reattach()

        assertNotNull("Source should exist after reattach", style.getSource("test-source"))
        assertTrue("Reattach should create a new instance", node.source !== firstSource)
    }

    @Test
    fun sourceNode_nullStyle_doesNotCrash() = onUiThread {
        val node = SourceNode(mutableStateOf(null)) { GeoJsonSource("test-source") }

        node.attach()
        node.onRemoved()
        node.onCleared()
    }

    // --- LayerNode ---

    @Test
    fun layerNode_attach_addsLayerToStyle() = withStyle { style ->
        val node = LayerNode(mutableStateOf(style)) { BackgroundLayer("test-layer") }

        node.attach()

        assertNotNull("Layer should exist in style after attach", style.getLayer("test-layer"))
    }

    @Test
    fun layerNode_onRemoved_removesLayerFromStyle() = withStyle { style ->
        val node = LayerNode(mutableStateOf(style)) { BackgroundLayer("test-layer") }
        node.attach()

        node.onRemoved()

        assertNull("Layer should not exist in style after onRemoved", style.getLayer("test-layer"))
    }

    @Test
    fun layerNode_onCleared_removesLayerFromStyle() = withStyle { style ->
        val node = LayerNode(mutableStateOf(style)) { BackgroundLayer("test-layer") }
        node.attach()

        node.onCleared()

        assertNull("Layer should not exist in style after onCleared", style.getLayer("test-layer"))
    }

    @Test
    fun layerNode_reattach_recreatesLayer() = withStyle { style ->
        val node = LayerNode(mutableStateOf(style)) { BackgroundLayer("test-layer") }
        node.attach()
        val firstLayer = node.layer

        node.onRemoved()
        node.reattach()

        assertNotNull("Layer should exist after reattach", style.getLayer("test-layer"))
        assertTrue("Reattach should create a new instance", node.layer !== firstLayer)
    }

    @Test
    fun layerNode_nullStyle_doesNotCrash() = onUiThread {
        val node = LayerNode(mutableStateOf(null)) { BackgroundLayer("test-layer") }

        node.attach()
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

    // --- SourceNode onUpdate ---

    @Test
    fun sourceNode_onUpdate_calledWithSource() = withStyle { style ->
        val node = SourceNode(mutableStateOf(style)) { GeoJsonSource("update-source") }
        node.attach()

        var receivedSource: org.maplibre.android.style.sources.Source? = null
        node.onUpdate = { receivedSource = it }
        node.source?.let { node.onUpdate?.invoke(it) }

        assertNotNull("onUpdate should receive the source", receivedSource)
        assertEquals("update-source", receivedSource?.id)
    }

    @Test
    fun sourceNode_onUpdate_notCalledWhenNull() = withStyle { style ->
        val node = SourceNode(mutableStateOf(style)) { GeoJsonSource("no-update-source") }
        node.attach()

        var called = false
        node.onUpdate = null
        node.onUpdate?.let { called = true }

        assertTrue("onUpdate should not be called when null", !called)
    }

    // --- LayerNode onUpdate ---

    @Test
    fun layerNode_onUpdate_calledWithLayer() = withStyle { style ->
        val node = LayerNode(mutableStateOf(style)) { BackgroundLayer("update-layer") }
        node.attach()

        var receivedLayer: org.maplibre.android.style.layers.Layer? = null
        node.onUpdate = { receivedLayer = it }
        node.layer?.let { node.onUpdate?.invoke(it) }

        assertNotNull("onUpdate should receive the layer", receivedLayer)
        assertEquals("update-layer", receivedLayer?.id)
    }

    @Test
    fun layerNode_onUpdate_notCalledWhenNull() = withStyle { style ->
        val node = LayerNode(mutableStateOf(style)) { BackgroundLayer("no-update-layer") }
        node.attach()

        var called = false
        node.onUpdate = null
        node.onUpdate?.let { called = true }

        assertTrue("onUpdate should not be called when null", !called)
    }
}
