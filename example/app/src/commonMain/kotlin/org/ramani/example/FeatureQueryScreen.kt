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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point
import org.ramani.compose.CameraPosition
import org.ramani.compose.Circle
import org.ramani.compose.LatLng
import org.ramani.compose.MapLibre
import org.ramani.compose.MapStyle
import org.ramani.compose.rememberCameraPositionState
import org.ramani.compose.rememberCenterState

private const val SWISSTOPO_STYLE =
    "https://vectortiles.geo.admin.ch/styles/ch.swisstopo.basemap.vt/style.json"

// Style-layer ids (not MVT source-layer names) for the swisstopo basemap's POIs, places and peaks.
private val SELECTABLE_LAYERS = setOf(
    "poi_rank1", "poi_rank2", "poi_rank3_rotation_flat", "poi_motorway",
    "place_city", "place_town_village", "place_other",
    "peak_rank1", "peak_rank2",
)

/**
 * Demonstrates [org.ramani.compose.MapProjection.queryRenderedFeatures]: tap a POI, place or peak on
 * the swisstopo basemap to select it. The tapped feature is highlighted and its attributes are shown
 * in a panel.
 */
@Composable
fun FeatureQueryScreen() {
    val cameraPositionState = rememberCameraPositionState(
        CameraPosition(target = LatLng(46.8, 8.2), zoom = 6.5)
    )
    var selected by remember { mutableStateOf<Feature<Geometry, JsonObject?>?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        MapLibre(
            modifier = Modifier.fillMaxSize(),
            style = MapStyle.Uri(SWISSTOPO_STYLE),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                cameraPositionState.projection?.let { projection ->
                    val screenPoint = projection.toScreenLocation(latLng)
                    selected = projection
                        .queryRenderedFeatures(screenPoint, radiusPx = 16f, layerIds = SELECTABLE_LAYERS)
                        .firstOrNull()
                }
            },
        ) {
            // Highlight the selected point feature with a ring.
            val highlight = (selected?.geometry as? Point)?.coordinates
            if (highlight != null) {
                key(highlight) {
                    Circle(
                        centerState = rememberCenterState(
                            LatLng(highlight.latitude, highlight.longitude)
                        ),
                        radius = 20f,
                        color = "#1E88E5",
                        opacity = 0.3f,
                        borderColor = "#1E88E5",
                        borderWidth = 3f,
                    )
                }
            }
        }

        selected?.let { feature ->
            FeatureInfoCard(
                feature = feature,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun FeatureInfoCard(feature: Feature<Geometry, JsonObject?>, modifier: Modifier = Modifier) {
    val properties = feature.properties
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = properties.displayName() ?: "Unnamed feature",
                style = MaterialTheme.typography.titleMedium,
            )
            listOf("class", "subclass", "ele").forEach { key ->
                properties.string(key)?.let { value ->
                    Text(
                        text = "$key: $value",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun JsonObject?.string(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.contentOrNull

private fun JsonObject?.displayName(): String? =
    string("name") ?: string("name_de") ?: string("name_latin")
