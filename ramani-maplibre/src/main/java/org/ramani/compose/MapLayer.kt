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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.maplibre.android.style.layers.Layer

/**
 * Adds a [Layer] to the map style.
 *
 * @param update Optional lambda called when recomposition occurs, receiving the current [Layer].
 *        Use this to update layer properties in-place — for example, calling `setProperties()`
 *        to change paint or layout values, or toggling visibility.
 * @param keys Values that should trigger the [update] callback when they change. Pass any state
 *        values read inside the [update] lambda so that changes are detected and applied.
 * @param factory Creates the layer. Called during initial composition and on style reload.
 */
@Composable
fun MapLayer(
    update: ((Layer) -> Unit)? = null,
    keys: List<Any?> = emptyList(),
    factory: () -> Layer,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<LayerNode, MapApplier>(
        factory = { LayerNode(mapApplier.style, factory).apply { attach() } },
        update = {
            set(update) {
                this.onUpdate = it
                it?.let { updater -> layer?.let(updater) }
            }
            set(keys) {
                this.onUpdate?.let { updater -> layer?.let(updater) }
            }
        }
    )
}
