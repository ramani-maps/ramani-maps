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
import org.maplibre.android.style.sources.Source

/**
 * Adds a [Source] to the map style.
 *
 * @param update Optional lambda called when recomposition occurs, receiving the current [Source].
 *        Use this to update the source in-place — for example, calling `setGeoJson()` on a
 *        [org.maplibre.android.style.sources.GeoJsonSource].
 * @param keys Values that should trigger the [update] callback when they change. Pass any state
 *        values read inside the [update] lambda so that changes are detected and applied.
 * @param factory Creates the source. Called during initial composition and on style reload.
 */
@Composable
fun MapSource(
    update: ((Source) -> Unit)? = null,
    keys: List<Any?> = emptyList(),
    factory: () -> Source,
) {
    val mapApplier = LocalMapApplier.current
    ComposeNode<SourceNode, MapApplier>(
        factory = { SourceNode(mapApplier.style, factory).apply { attach() } },
        update = {
            set(update) {
                this.onUpdate = it
                it?.let { updater -> source?.let(updater) }
            }
            set(keys) {
                this.onUpdate?.let { updater -> source?.let(updater) }
            }
        }
    )
}
