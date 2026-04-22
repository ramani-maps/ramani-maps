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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MapStyleTest {

    // --- Equality (the whole point of the wrapper) ---

    @Test
    fun uri_sameUrl_areEqual() {
        val a = MapStyle.Uri("https://example.com/style.json")
        val b = MapStyle.Uri("https://example.com/style.json")
        assertEquals(a, b)
    }

    @Test
    fun uri_differentUrl_areNotEqual() {
        val a = MapStyle.Uri("https://example.com/style1.json")
        val b = MapStyle.Uri("https://example.com/style2.json")
        assertNotEquals(a, b)
    }

    @Test
    fun json_sameJson_areEqual() {
        val json = """{"version": 8, "sources": {}, "layers": []}"""
        val a = MapStyle.Json(json)
        val b = MapStyle.Json(json)
        assertEquals(a, b)
    }

    @Test
    fun json_differentJson_areNotEqual() {
        val a = MapStyle.Json("""{"version": 8, "sources": {}, "layers": []}""")
        val b = MapStyle.Json("""{"version": 8, "sources": {"foo": {}}, "layers": []}""")
        assertNotEquals(a, b)
    }

    @Test
    fun uri_and_json_areNotEqual() {
        val uri = MapStyle.Uri("https://example.com/style.json")
        val json = MapStyle.Json("https://example.com/style.json")
        assertNotEquals(uri, json)
    }

    // --- hashCode consistency ---

    @Test
    fun uri_sameUrl_sameHashCode() {
        val a = MapStyle.Uri("https://example.com/style.json")
        val b = MapStyle.Uri("https://example.com/style.json")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun json_sameJson_sameHashCode() {
        val json = """{"version": 8, "sources": {}, "layers": []}"""
        assertEquals(MapStyle.Json(json).hashCode(), MapStyle.Json(json).hashCode())
    }

    // --- Default style ---

    @Test
    fun defaultStyle_isUri() {
        val style = MapStyle.Default
        assertEquals(MapStyle.Uri("https://demotiles.maplibre.org/style.json"), style)
    }
}
