package org.ramani.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode

@MapLibreComposable
@Composable
fun MapObserver(onMapMoved: () -> Unit = {}, onMapScaled: () -> Unit = {}) {
    ComposeNode<MapObserverNode, MapApplier>(factory = {
        MapObserverNode(onMapMoved, onMapScaled)
    },
        update = {
            update(onMapMoved) {
                this.onMapMoved = { onMapMoved }
            }

            update(onMapScaled) {
                this.onMapScaled = { onMapScaled }
            }
        })
}