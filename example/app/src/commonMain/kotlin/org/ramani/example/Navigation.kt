/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ExampleNavHost() {
    var currentScreen by rememberSaveable { mutableStateOf("home") }
    val goHome = { currentScreen = "home" }

    when (currentScreen) {
        "home" -> ExampleListScreen(onNavigate = { currentScreen = it })
        "annotation" -> MapScreenScaffold(onBack = goHome) { AnnotationScreen() }
        "clusters" -> MapScreenScaffold(onBack = goHome) { ClustersScreen() }
        "custom-annotation" -> MapScreenScaffold(onBack = goHome) { CustomAnnotationScreen() }
        "custom-layers" -> MapScreenScaffold(onBack = goHome) { CustomLayersScreen() }
        "interactive-polygon" -> MapScreenScaffold(onBack = goHome) { InteractivePolygonScreen() }
        "location" -> MapScreenScaffold(onBack = goHome) { LocationScreen() }
    }
}

/**
 * Wraps an example screen with a back affordance:
 *  - a [BackHandler] so the Android system back gesture/button returns to the example
 *    list instead of leaving the app,
 *  - a floating back button (top-start) so iOS, which has no system back gesture here,
 *    can navigate back too.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MapScreenScaffold(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        }
    }
}
