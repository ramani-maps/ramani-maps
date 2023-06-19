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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "Maplibre Composable")
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class MapLibreComposable

@Composable
fun MapLibre(
    modifier: Modifier,
    apiKey: String,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    content: (@Composable @MapLibreComposable () -> Unit)? = null,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    val map = rememberMapViewWithLifecycle()
    val currentContent by rememberUpdatedState(content)
    val parentComposition = rememberCompositionContext()

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { map })
    LaunchedEffect(Unit) {
        disposingComposition {
            map.newComposition(parentComposition, style = map.awaitMap().awaitStyle(apiKey)) {
                CompositionLocalProvider() {
                    currentContent?.invoke()
                }
            }
        }
    }
}
