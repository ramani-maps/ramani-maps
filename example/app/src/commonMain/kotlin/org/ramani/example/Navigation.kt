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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun ExampleNavHost() {
    var currentScreen by rememberSaveable { mutableStateOf("home") }

    when (currentScreen) {
        "home" -> ExampleListScreen(onNavigate = { currentScreen = it })
        "annotation" -> AnnotationScreen()
        "clusters" -> ClustersScreen()
        "custom-annotation" -> CustomAnnotationScreen()
        "custom-layers" -> CustomLayersScreen()
        "interactive-polygon" -> InteractivePolygonScreen()
        "location" -> LocationScreen()
    }
}
