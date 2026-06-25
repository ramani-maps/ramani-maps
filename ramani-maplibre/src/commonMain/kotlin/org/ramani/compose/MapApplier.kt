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

import androidx.compose.runtime.AbstractApplier

interface MapNode {
    fun onAttached() {}
    fun onRemoved() {}
    fun onCleared() {}
}

private object MapNodeRoot : MapNode

abstract class BaseMapApplier : AbstractApplier<MapNode>(MapNodeRoot) {
    protected val decorations = mutableListOf<MapNode>()
    private val layerAliases = mutableMapOf<String, String>()

    fun registerLayerAlias(alias: String, targetLayerId: String) {
        layerAliases[alias] = targetLayerId
    }

    fun resolveLayerAlias(alias: String): String = layerAliases[alias] ?: alias

    override fun insertBottomUp(index: Int, instance: MapNode) {
        // Ignored
    }

    override fun insertTopDown(index: Int, instance: MapNode) {
        decorations.add(index, instance)
        instance.onAttached()
    }

    override fun move(from: Int, to: Int, count: Int) {
    }

    override fun onClear() {
        decorations.forEach { it.onCleared() }
        decorations.clear()
        layerAliases.clear()
    }

    override fun remove(index: Int, count: Int) {
        val toRemove = decorations.subList(index, index + count)
        toRemove.forEach { it.onRemoved() }
        toRemove.clear()
    }
}

internal data class PendingLayerOrder(
    val layerId: String,
    val aboveLayerId: String?,
    val belowLayerId: String?
)

internal fun computeLayerOrder(
    pendingOrders: List<PendingLayerOrder>,
    registeredLayerIds: Set<String>,
    declarationOrder: Map<String, Int>
): List<String> {
    // Collect every layer that participates in ordering (subjects + references).
    val involvedLayerIds = mutableSetOf<String>()
    for (order in pendingOrders) {
        involvedLayerIds.add(order.layerId)
        order.aboveLayerId?.let { involvedLayerIds.add(it) }
        order.belowLayerId?.let { involvedLayerIds.add(it) }
    }
    involvedLayerIds.retainAll(registeredLayerIds)

    // Build a DAG: edge A -> B means "A must be below B in the final stack".
    val adj = involvedLayerIds.associateWith { mutableListOf<String>() }
    val inDegree = involvedLayerIds.associateWithTo(mutableMapOf()) { 0 }

    for (order in pendingOrders) {
        val layerId = order.layerId
        val above = order.aboveLayerId
        val below = order.belowLayerId

        if (above != null && above in involvedLayerIds && layerId in involvedLayerIds) {
            adj[above]!!.add(layerId)
            inDegree[layerId] = inDegree[layerId]!! + 1
        }
        if (below != null && below in involvedLayerIds && layerId in involvedLayerIds) {
            adj[layerId]!!.add(below)
            inDegree[below] = inDegree[below]!! + 1
        }
    }

    // Kahn's algorithm with declaration-order tiebreaker: when several layers
    // have no remaining dependencies the one declared earliest goes first
    // (= lower in the layer stack), preserving compose-tree order for
    // independent layers.
    val queue = inDegree.filter { it.value == 0 }.keys
        .sortedBy { declarationOrder[it] ?: Int.MAX_VALUE }
        .toMutableList()

    val sortedOrder = mutableListOf<String>()
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        sortedOrder.add(current)
        for (neighbor in adj[current]!!) {
            val newDeg = inDegree[neighbor]!! - 1
            inDegree[neighbor] = newDeg
            if (newDeg == 0) {
                // Insert in declaration-order position
                val neighborOrder = declarationOrder[neighbor] ?: Int.MAX_VALUE
                val insertIdx = queue.indexOfFirst {
                    (declarationOrder[it] ?: Int.MAX_VALUE) > neighborOrder
                }
                if (insertIdx == -1) queue.add(neighbor) else queue.add(insertIdx, neighbor)
            }
        }
    }

    return sortedOrder
}
