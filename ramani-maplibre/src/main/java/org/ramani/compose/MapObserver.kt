/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2023 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode

@MapLibreComposable
@Composable
fun MapObserver(
    onMapMoved: () -> Unit = {},
    onMapScaled: () -> Unit = {},
    onMapRotated: (Double) -> Unit
) {
    ComposeNode<MapObserverNode, MapApplier>(
        factory = {
            MapObserverNode(onMapMoved, onMapScaled, onMapRotated)
        },
        update = {
            update(onMapMoved) {
                this.onMapMoved = onMapMoved
            }

            update(onMapScaled) {
                this.onMapScaled = onMapScaled
            }

            update(onMapRotated) {
                this.onMapRotated = onMapRotated
            }
        }
    )
}
