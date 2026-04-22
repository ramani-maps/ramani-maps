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
import androidx.compose.ui.platform.LocalContext

@Composable
fun MapImage(id: String, drawableRes: Int) {
    val context = LocalContext.current
    val mapApplier = currentComposer.applier as MapApplier
    ComposeNode<ImageNode, MapApplier>(
        factory = { ImageNode(mapApplier.style, context, id, drawableRes) },
        update = {}
    )
}
