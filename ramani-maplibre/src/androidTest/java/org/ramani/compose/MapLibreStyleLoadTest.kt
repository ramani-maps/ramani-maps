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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class MapLibreStyleLoadTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ComposeTestActivity::class.java)

    private val blankStyleJson = """{"version": 8, "sources": {}, "layers": []}"""

    @Test
    fun onStyleLoaded_calledExactlyOnce_onInitialComposition() {
        val styleLoadCount = AtomicInteger(0)
        val latch = CountDownLatch(1)

        activityRule.scenario.onActivity { activity ->
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = MapStyle.Json(blankStyleJson),
                    onStyleLoaded = {
                        styleLoadCount.incrementAndGet()
                        latch.countDown()
                    },
                )
            }
        }

        assertTrue("Style should load within 15s", latch.await(15, TimeUnit.SECONDS))
        // Give a short window for any spurious second call
        Thread.sleep(2000)
        assertEquals("onStyleLoaded should be called exactly once on init", 1, styleLoadCount.get())
    }

    @Test
    fun onStyleLoaded_calledAgain_whenStyleChanges() {
        val styleLoadCount = AtomicInteger(0)
        val secondLoadLatch = CountDownLatch(1)
        val currentStyle = mutableStateOf<MapStyle>(MapStyle.Json(blankStyleJson))

        activityRule.scenario.onActivity { activity ->
            activity.setContent {
                MapLibre(
                    modifier = Modifier.fillMaxSize(),
                    style = currentStyle.value,
                    onStyleLoaded = {
                        val count = styleLoadCount.incrementAndGet()
                        if (count == 2) secondLoadLatch.countDown()
                    },
                )
            }
        }

        // Wait for initial load
        val initialLatch = CountDownLatch(1)
        Thread {
            while (styleLoadCount.get() < 1) Thread.sleep(100)
            initialLatch.countDown()
        }.start()
        assertTrue("Initial style should load within 15s", initialLatch.await(15, TimeUnit.SECONDS))

        // Change the style
        activityRule.scenario.onActivity {
            currentStyle.value = MapStyle.Json("""{"version": 8, "sources": {}, "layers": [], "name": "changed"}""")
        }

        assertTrue("Style change should trigger onStyleLoaded within 15s", secondLoadLatch.await(15, TimeUnit.SECONDS))
        assertEquals("onStyleLoaded should be called exactly twice (init + change)", 2, styleLoadCount.get())
    }
}
