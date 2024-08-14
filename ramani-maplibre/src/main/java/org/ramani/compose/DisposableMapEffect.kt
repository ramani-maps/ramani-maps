/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2024 Rin Luu.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.currentComposer
import org.maplibre.android.maps.MapView

/**
 * A composable function that performs a disposable side-effect on a `MapView` when the specified key changes.
 *
 * This function allows you to execute a block of code that operates on the `MapView` instance and returns a `DisposableEffectResult`.
 * The side-effect is managed by `DisposableEffect`, which ensures proper cleanup when the key changes or the composable is disposed.
 *
 * @param key1 A key used to control when the disposable side-effect should be executed. If `key1` changes, the block of code will be executed again. Can be `null`.
 * @param block A lambda function that receives the `MapView` instance as a parameter and returns a `DisposableEffectResult`. The function is executed within a `DisposableEffectScope`.
 *
 * @return A `DisposableEffectResult` that is used for managing the lifecycle of the side-effect.
 *
 * @see MapView for details on the map view used in this composable.
 * @see DisposableEffect for more information on disposable side-effects.
 *
 * @example
 * DisposableMapEffect(key1 = someKey) { mapView ->
 *     // Perform operations on mapView and return a cleanup action
 *     val marker = mapView.addMarker(...)
 *     onDispose {
 *         marker.remove()
 *     }
 * }
 */
@Composable
@MapLibreComposable
fun DisposableMapEffect(key1: Any?, block: DisposableEffectScope.(MapView) -> DisposableEffectResult) {
    val map = (currentComposer.applier as MapApplier).mapView
    DisposableEffect(key1 = key1) {
        block(map)
    }
}

/**
 * A composable function that performs a disposable side-effect on a `MapView` when either of the specified keys changes.
 *
 * This function allows you to execute a block of code that operates on the `MapView` instance and returns a `DisposableEffectResult`.
 * The side-effect is managed by `DisposableEffect`, ensuring proper cleanup when either `key1` or `key2` changes or the composable is disposed.
 *
 * @param key1 A key used to control when the disposable side-effect should be executed. If `key1` changes, the block of code will be executed again. Can be `null`.
 * @param key2 Another key used to control when the disposable side-effect should be executed. If `key2` changes, the block of code will be executed again. Can be `null`.
 * @param block A lambda function that receives the `MapView` instance as a parameter and returns a `DisposableEffectResult`. The function is executed within a `DisposableEffectScope`.
 *
 * @return A `DisposableEffectResult` used for managing the lifecycle of the side-effect.
 *
 * @see MapView for details on the map view used in this composable.
 * @see DisposableEffect for more information on disposable side-effects.
 *
 * @example
 * DisposableMapEffect(key1 = someKey1, key2 = someKey2) { mapView ->
 *     // Perform operations on mapView and return a cleanup action
 *     val marker = mapView.addMarker(...)
 *     onDispose {
 *         marker.remove()
 *     }
 * }
 */
@Composable
@MapLibreComposable
fun DisposableMapEffect(key1: Any?, key2: Any?, block: DisposableEffectScope.(MapView) -> DisposableEffectResult) {
    val map = (currentComposer.applier as MapApplier).mapView
    DisposableEffect(key1 = key1, key2 = key2) {
        block(map)
    }
}

/**
 * A composable function that performs a disposable side-effect on a `MapView` when any of the specified keys change.
 *
 * This function executes a block of code that operates on the `MapView` instance and returns a `DisposableEffectResult`.
 * The side-effect is managed by `DisposableEffect`, ensuring proper cleanup when any of the specified keys change or when the composable is disposed.
 *
 * @param key1 A key used to control when the disposable side-effect should be executed. If `key1` changes, the block of code will be executed again. Can be `null`.
 * @param key2 A key used to control when the disposable side-effect should be executed. If `key2` changes, the block of code will be executed again. Can be `null`.
 * @param key3 A key used to control when the disposable side-effect should be executed. If `key3` changes, the block of code will be executed again. Can be `null`.
 * @param block A lambda function that receives the `MapView` instance as a parameter and returns a `DisposableEffectResult`. The function is executed within a `DisposableEffectScope`.
 *
 * @return A `DisposableEffectResult` used for managing the lifecycle of the side-effect.
 *
 * @see MapView for details on the map view used in this composable.
 * @see DisposableEffect for more information on disposable side-effects.
 *
 * @example
 * DisposableMapEffect(key1 = someKey1, key2 = someKey2, key3 = someKey3) { mapView ->
 *     // Perform operations on mapView and return a cleanup action
 *     val marker = mapView.addMarker(...)
 *     onDispose {
 *         marker.remove()
 *     }
 * }
 */
@Composable
@MapLibreComposable
fun DisposableMapEffect(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    block: DisposableEffectScope.(MapView) -> DisposableEffectResult
) {
    val map = (currentComposer.applier as MapApplier).mapView
    DisposableEffect(key1 = key1, key2 = key2, key3 = key3) {
        block(map)
    }
}

/**
 * A composable function that performs a disposable side-effect on a `MapView` when any of the provided keys change.
 *
 * This function executes a block of code that operates on the `MapView` instance and returns a `DisposableEffectResult`.
 * The side-effect is managed by `DisposableEffect`, ensuring proper cleanup when any of the specified keys change or when the composable is disposed.
 *
 * @param keys A vararg parameter of keys used to control when the disposable side-effect should be executed. If any of the keys change, the block of code will be executed again. Keys can be `null`.
 * @param block A lambda function that receives the `MapView` instance as a parameter and returns a `DisposableEffectResult`. The function is executed within a `DisposableEffectScope`.
 *
 * @return A `DisposableEffectResult` used for managing the lifecycle of the side-effect.
 *
 * @see MapView for details on the map view used in this composable.
 * @see DisposableEffect for more information on disposable side-effects.
 *
 * @example
 * DisposableMapEffect(key1, key2, key3) { mapView ->
 *     // Perform operations on mapView and return a cleanup action
 *     val marker = mapView.addMarker(...)
 *     onDispose {
 *         marker.remove()
 *     }
 * }
 */
@Composable
@MapLibreComposable
fun DisposableMapEffect(vararg keys: Any?, block: DisposableEffectScope.(MapView) -> DisposableEffectResult) {
    val map = (currentComposer.applier as MapApplier).mapView
    DisposableEffect(keys = keys) {
        block(map)
    }
}