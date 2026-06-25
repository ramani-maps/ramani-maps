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
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerOrderTest {

    // --- Basic cases ---

    @Test
    fun emptyInput_returnsEmptyList() {
        val result = computeLayerOrder(
            pendingOrders = emptyList(),
            registeredLayerIds = setOf("A", "B"),
            declarationOrder = mapOf("A" to 0, "B" to 1)
        )
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun singleLayer_noConstraints_returnsThatLayer() {
        val result = computeLayerOrder(
            pendingOrders = listOf(PendingLayerOrder("A", null, null)),
            registeredLayerIds = setOf("A"),
            declarationOrder = mapOf("A" to 0)
        )
        assertEquals(listOf("A"), result)
    }

    // --- aboveLayerId ---

    @Test
    fun aboveLayerId_placesLayerAboveReference() {
        // "B above A" means A is below B => order: [A, B]
        val result = computeLayerOrder(
            pendingOrders = listOf(PendingLayerOrder("B", aboveLayerId = "A", belowLayerId = null)),
            registeredLayerIds = setOf("A", "B"),
            declarationOrder = mapOf("A" to 0, "B" to 1)
        )
        assertEquals(listOf("A", "B"), result)
    }

    @Test
    fun aboveLayerId_reversesDeclarationOrder() {
        // A declared first but "A above B" => order: [B, A]
        val result = computeLayerOrder(
            pendingOrders = listOf(PendingLayerOrder("A", aboveLayerId = "B", belowLayerId = null)),
            registeredLayerIds = setOf("A", "B"),
            declarationOrder = mapOf("A" to 0, "B" to 1)
        )
        assertEquals(listOf("B", "A"), result)
    }

    // --- belowLayerId ---

    @Test
    fun belowLayerId_placesLayerBelowReference() {
        // "A below B" means A is below B => order: [A, B]
        val result = computeLayerOrder(
            pendingOrders = listOf(PendingLayerOrder("A", aboveLayerId = null, belowLayerId = "B")),
            registeredLayerIds = setOf("A", "B"),
            declarationOrder = mapOf("A" to 0, "B" to 1)
        )
        assertEquals(listOf("A", "B"), result)
    }

    @Test
    fun belowLayerId_reversesDeclarationOrder() {
        // B declared second but "B below A" => order: [B, A]
        val result = computeLayerOrder(
            pendingOrders = listOf(PendingLayerOrder("B", aboveLayerId = null, belowLayerId = "A")),
            registeredLayerIds = setOf("A", "B"),
            declarationOrder = mapOf("A" to 0, "B" to 1)
        )
        assertEquals(listOf("B", "A"), result)
    }

    // --- Linear chain ---

    @Test
    fun linearChain_aboveConstraints() {
        // C above B, B above A => order: [A, B, C]
        val result = computeLayerOrder(
            pendingOrders = listOf(
                PendingLayerOrder("C", aboveLayerId = "B", belowLayerId = null),
                PendingLayerOrder("B", aboveLayerId = "A", belowLayerId = null),
            ),
            registeredLayerIds = setOf("A", "B", "C"),
            declarationOrder = mapOf("A" to 0, "B" to 1, "C" to 2)
        )
        assertEquals(listOf("A", "B", "C"), result)
    }

    @Test
    fun linearChain_belowConstraints() {
        // A below B, B below C => order: [A, B, C]
        val result = computeLayerOrder(
            pendingOrders = listOf(
                PendingLayerOrder("A", aboveLayerId = null, belowLayerId = "B"),
                PendingLayerOrder("B", aboveLayerId = null, belowLayerId = "C"),
            ),
            registeredLayerIds = setOf("A", "B", "C"),
            declarationOrder = mapOf("A" to 0, "B" to 1, "C" to 2)
        )
        assertEquals(listOf("A", "B", "C"), result)
    }

    // --- Declaration order tiebreaker ---

    @Test
    fun independentLayers_preserveDeclarationOrder() {
        // Both constrained relative to a common anchor but not to each other
        // B above A, C above A => B(decl=1) before C(decl=2)
        val result = computeLayerOrder(
            pendingOrders = listOf(
                PendingLayerOrder("B", aboveLayerId = "A", belowLayerId = null),
                PendingLayerOrder("C", aboveLayerId = "A", belowLayerId = null),
            ),
            registeredLayerIds = setOf("A", "B", "C"),
            declarationOrder = mapOf("A" to 0, "B" to 1, "C" to 2)
        )
        assertEquals(listOf("A", "B", "C"), result)
    }

    @Test
    fun independentLayers_laterDeclarationComesLater() {
        // Same as above but declaration order reversed
        val result = computeLayerOrder(
            pendingOrders = listOf(
                PendingLayerOrder("B", aboveLayerId = "A", belowLayerId = null),
                PendingLayerOrder("C", aboveLayerId = "A", belowLayerId = null),
            ),
            registeredLayerIds = setOf("A", "B", "C"),
            declarationOrder = mapOf("A" to 0, "C" to 1, "B" to 2)
        )
        assertEquals(listOf("A", "C", "B"), result)
    }

    // --- Missing references ---

    @Test
    fun missingReference_aboveUnknownLayer_layerStillAppears() {
        // "B above X" but X is not registered => B still appears (no constraint applied)
        val result = computeLayerOrder(
            pendingOrders = listOf(PendingLayerOrder("B", aboveLayerId = "X", belowLayerId = null)),
            registeredLayerIds = setOf("B"),
            declarationOrder = mapOf("B" to 0)
        )
        assertEquals(listOf("B"), result)
    }

    @Test
    fun missingReference_belowUnknownLayer_layerStillAppears() {
        val result = computeLayerOrder(
            pendingOrders = listOf(PendingLayerOrder("A", aboveLayerId = null, belowLayerId = "X")),
            registeredLayerIds = setOf("A"),
            declarationOrder = mapOf("A" to 0)
        )
        assertEquals(listOf("A"), result)
    }

    // --- Mixed above/below ---

    @Test
    fun mixedAboveAndBelow_producesCorrectOrder() {
        // B above A, B below C => order: [A, B, C]
        val result = computeLayerOrder(
            pendingOrders = listOf(
                PendingLayerOrder("B", aboveLayerId = "A", belowLayerId = "C"),
            ),
            registeredLayerIds = setOf("A", "B", "C"),
            declarationOrder = mapOf("A" to 0, "B" to 1, "C" to 2)
        )
        assertEquals(listOf("A", "B", "C"), result)
    }

    // --- Cycle detection ---

    @Test
    fun cycle_doesNotCrash() {
        // A above B, B above A => cycle; Kahn's drops them both
        val result = computeLayerOrder(
            pendingOrders = listOf(
                PendingLayerOrder("A", aboveLayerId = "B", belowLayerId = null),
                PendingLayerOrder("B", aboveLayerId = "A", belowLayerId = null),
            ),
            registeredLayerIds = setOf("A", "B"),
            declarationOrder = mapOf("A" to 0, "B" to 1)
        )
        // With a cycle, Kahn's algorithm drops the cycled nodes from the output.
        // The important thing is it doesn't crash or loop infinitely.
        assertTrue(result.size < 2)
    }

    // --- Forward references ---

    @Test
    fun forwardReference_layerDeclaredBeforeReference() {
        // B declared first (index 0), A declared second (index 1)
        // "B above A" => order: [A, B]
        val result = computeLayerOrder(
            pendingOrders = listOf(PendingLayerOrder("B", aboveLayerId = "A", belowLayerId = null)),
            registeredLayerIds = setOf("A", "B"),
            declarationOrder = mapOf("B" to 0, "A" to 1)
        )
        assertEquals(listOf("A", "B"), result)
    }
}
