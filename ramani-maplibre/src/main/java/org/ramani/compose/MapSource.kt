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
import androidx.compose.runtime.currentComposer
import org.maplibre.android.style.sources.Source

@Composable
fun MapSource(factory: () -> Source) {
    val mapApplier = currentComposer.applier as MapApplier
    ComposeNode<SourceNode, MapApplier>(
        factory = { SourceNode(mapApplier.style, factory).apply { attach() } },
        update = {}
    )
}
