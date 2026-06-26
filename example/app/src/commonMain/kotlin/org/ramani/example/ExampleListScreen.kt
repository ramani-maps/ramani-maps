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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Example(val route: String, val title: String, val subtitle: String)

private val examples = listOf(
    Example("annotation", "Annotations", "Circle, polyline and symbol"),
    Example("clusters", "Clusters", "GeoJSON clustering with earthquake data"),
    Example("custom-annotation", "Custom Annotation", "Progress circle built from primitives"),
    Example("custom-layers", "Custom Layers", "Vector, raster and GeoJSON layers"),
    Example("feature-query", "Feature Query", "Tap to select POIs / places / peaks (swisstopo)"),
    Example("interactive-polygon", "Interactive Polygon", "Draggable polygon with vertex handles"),
    Example("location", "Location", "Device location tracking"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleListScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ramani Examples") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(examples) { example ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onNavigate(example.route) },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = example.title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = example.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
