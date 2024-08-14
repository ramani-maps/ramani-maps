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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import kotlinx.coroutines.CoroutineScope
import org.maplibre.android.maps.MapView

/**
 * A composable function that performs a side-effect on a `MapView` when the `key1` parameter changes.
 *
 * This function allows you to execute a suspendable block of code that operates on the `MapView` instance.
 * The `MapView` instance is obtained from the `MapApplier`, and the side-effect is triggered whenever `key1` changes.
 *
 * @param key1 A key used to control when the side-effect should be executed. If `key1` changes, the block of code will be executed again. Can be `null`.
 * @param block A suspendable lambda function that receives the `MapView` instance as a parameter and performs the desired side-effect. The function is executed within a coroutine scope.
 *
 * @see MapView for details on the map view used in this composable.
 *
 * @example
 * MapEffect(key1 = someKey) { mapView ->
 *     // Perform operations on mapView
 *     mapView.addMarker(...)
 * }
 */
@Composable
@MapLibreComposable
fun MapEffect(key1: Any?, block: suspend CoroutineScope.(MapView) -> Unit) {
    val map = (currentComposer.applier as MapApplier).mapView
    LaunchedEffect(key1 = key1) {
        block(map)
    }
}

/**
 * A composable function that performs a side-effect on a `MapView` when either of the `key1` or `key2` parameters changes.
 *
 * This function allows you to execute a suspendable block of code that operates on the `MapView` instance.
 * The `MapView` instance is obtained from the `MapApplier`, and the side-effect is triggered whenever either `key1` or `key2` changes.
 *
 * @param key1 A key used to control when the side-effect should be executed. If `key1` changes, the block of code will be executed again. Can be `null`.
 * @param key2 Another key used to control when the side-effect should be executed. If `key2` changes, the block of code will be executed again. Can be `null`.
 * @param block A suspendable lambda function that receives the `MapView` instance as a parameter and performs the desired side-effect. The function is executed within a coroutine scope.
 *
 * @see MapView for details on the map view used in this composable.
 *
 * @example
 * MapEffect(key1 = someKey1, key2 = someKey2) { mapView ->
 *     // Perform operations on mapView
 *     mapView.addMarker(...)
 * }
 */
@Composable
@MapLibreComposable
fun MapEffect(key1: Any?, key2: Any?, block: suspend CoroutineScope.(MapView) -> Unit) {
    val map = (currentComposer.applier as MapApplier).mapView
    LaunchedEffect(key1 = key1, key2 = key2) {
        block(map)
    }
}

/**
 * A composable function that performs a side-effect on a `MapView` when any of the `key1`, `key2`, or `key3` parameters change.
 *
 * This function allows you to execute a suspendable block of code that operates on the `MapView` instance.
 * The `MapView` instance is obtained from the `MapApplier`, and the side-effect is triggered whenever any of the specified keys change.
 *
 * @param key1 A key used to control when the side-effect should be executed. If `key1` changes, the block of code will be executed again. Can be `null`.
 * @param key2 A key used to control when the side-effect should be executed. If `key2` changes, the block of code will be executed again. Can be `null`.
 * @param key3 A key used to control when the side-effect should be executed. If `key3` changes, the block of code will be executed again. Can be `null`.
 * @param block A suspendable lambda function that receives the `MapView` instance as a parameter and performs the desired side-effect. The function is executed within a coroutine scope.
 *
 * @see MapView for details on the map view used in this composable.
 *
 * @example
 * MapEffect(key1 = someKey1, key2 = someKey2, key3 = someKey3) { mapView ->
 *     // Perform operations on mapView
 *     mapView.addMarker(...)
 * }
 */
@Composable
@MapLibreComposable
fun MapEffect(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    block: suspend CoroutineScope.(MapView) -> Unit
) {
    val map = (currentComposer.applier as MapApplier).mapView
    LaunchedEffect(key1 = key1, key2 = key2, key3 = key3) {
        block(map)
    }
}

/**
 * A composable function that performs a side-effect on a `MapView` when any of the provided keys change.
 *
 * This function allows you to execute a suspendable block of code that operates on the `MapView` instance.
 * The `MapView` instance is obtained from the `MapApplier`, and the side-effect is triggered whenever any of the specified keys change.
 *
 * @param keys A vararg parameter of keys used to control when the side-effect should be executed. If any of the keys change, the block of code will be executed again. Keys can be `null`.
 * @param block A suspendable lambda function that receives the `MapView` instance as a parameter and performs the desired side-effect. The function is executed within a coroutine scope.
 *
 * @see MapView for details on the map view used in this composable.
 *
 * @example
 * MapEffect(key1, key2, key3) { mapView ->
 *     // Perform operations on mapView
 *     mapView.addMarker(...)
 * }
 */
@Composable
@MapLibreComposable
fun MapEffect(vararg keys: Any?, block: suspend CoroutineScope.(MapView) -> Unit) {
    val map = (currentComposer.applier as MapApplier).mapView
    LaunchedEffect(keys = keys) {
        block(map)
    }
}