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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ramani.compose.CameraPosition
import org.ramani.compose.LatLng
import org.ramani.compose.MapLibre
import org.ramani.compose.MapStyle
import org.ramani.compose.UiSettings
import org.ramani.compose.rememberCameraPositionState
import org.ramani.compose.style.CircleStyleLayer
import org.ramani.compose.style.Expression
import org.ramani.compose.style.FillStyleLayer
import org.ramani.compose.style.GeoJsonSource
import org.ramani.compose.style.LineStyleLayer
import org.ramani.compose.style.RasterSource
import org.ramani.compose.style.RasterStyleLayer
import org.ramani.compose.style.VectorSource

private const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"

@Composable
fun CustomLayersScreen() {
    val maplibreStyleUrl = ApiKeys.MAPLIBRE_STYLE_URL
    val thunderKey = ApiKeys.THUNDERFOREST_API_KEY
    val maptilerKey = ApiKeys.MAPTILER_API_KEY

    val cameraPositionState = rememberCameraPositionState(
        CameraPosition(target = LatLng(46.6, 7.1), zoom = 8.0)
    )
    val isDefaultStyle = rememberSaveable { mutableStateOf(true) }
    val styleUrl = rememberSaveable { mutableStateOf(DEFAULT_STYLE_URL) }
    val uiSettings = remember {
        mutableStateOf(UiSettings(rotateGesturesEnabled = false))
    }

    var showNoFlyZones by rememberSaveable { mutableStateOf(true) }
    var showHillshades by rememberSaveable { mutableStateOf(true) }
    var showContours by rememberSaveable { mutableStateOf(false) }
    var circleRadius by rememberSaveable { mutableFloatStateOf(8f) }

    Box {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MapLibre(
                modifier = Modifier.fillMaxSize(),
                style = MapStyle.Uri(styleUrl.value),
                uiSettings = uiSettings.value,
                cameraPositionState = cameraPositionState,
            ) {
                if (showNoFlyZones) {
                    GeoJsonSource(
                        id = "no-fly-zones",
                        uri = "https://data.geo.admin.ch/ch.bazl.einschraenkungen-drohnen/einschraenkungen-drohnen/einschraenkungen-drohnen_4326.geojson",
                    )
                    FillStyleLayer(
                        id = "nfz-fill",
                        sourceId = "no-fly-zones",
                        color = Expression.switchCase(
                            Expression.has("fill"),
                            Expression.get("fill"),
                            Expression.color(0xFF0000FF.toInt()),
                        ),
                        opacity = Expression.switchCase(
                            Expression.has("fill-opacity"),
                            Expression.get("fill-opacity"),
                            Expression.const(0.4f),
                        ),
                    )
                    LineStyleLayer(
                        id = "nfz-poly",
                        sourceId = "no-fly-zones",
                        color = Expression.switchCase(
                            Expression.has("stroke"),
                            Expression.get("stroke"),
                            Expression.color(0xFF0000FF.toInt()),
                        ),
                        width = Expression.switchCase(
                            Expression.has("stroke-width"),
                            Expression.get("stroke-width"),
                            Expression.const(2),
                        ),
                        opacity = Expression.switchCase(
                            Expression.has("stroke-opacity"),
                            Expression.get("stroke-opacity"),
                            Expression.const(0.4f),
                        ),
                    )
                }

                if (showHillshades && maptilerKey.isNotEmpty()) {
                    RasterSource(
                        id = "hillshades",
                        tileJsonUrl = "https://api.maptiler.com/tiles/hillshade/tiles.json?key=$maptilerKey",
                    )
                    RasterStyleLayer(
                        id = "hillshades-layer",
                        sourceId = "hillshades",
                    )
                }

                if (showContours && thunderKey.isNotEmpty()) {
                    VectorSource(
                        id = "contours",
                        tileJsonUrl = "https://tile.thunderforest.com/thunderforest.outdoors-v2.json?apikey=$thunderKey",
                    )
                    LineStyleLayer(
                        id = "contour",
                        sourceId = "contours",
                        sourceLayer = "elevation",
                    )
                }

                GeoJsonSource(
                    id = "circle-points",
                    geoJson = circlePointsGeoJson(),
                )
                CircleStyleLayer(
                    id = "circle-points-layer",
                    sourceId = "circle-points",
                    radius = Expression.const(circleRadius),
                    color = Expression.color(radiusToColor(circleRadius)),
                    opacity = Expression.const(0.7f),
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Radius: ${circleRadius.toInt()}",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Slider(
                    value = circleRadius,
                    onValueChange = { circleRadius = it },
                    valueRange = 2f..40f,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ToggleButton("NFZ", showNoFlyZones) { showNoFlyZones = it }
                ToggleButton("Hills", showHillshades) { showHillshades = it }
                ToggleButton("Contours", showContours) { showContours = it }
            }

            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    uiSettings.value =
                        UiSettings(rotateGesturesEnabled = !uiSettings.value.rotateGesturesEnabled)
                }) {
                if (uiSettings.value.rotateGesturesEnabled) {
                    Text("Disable map rotation")
                } else {
                    Text("Enable map rotation")
                }
            }
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    styleUrl.value =
                        if (!isDefaultStyle.value) DEFAULT_STYLE_URL
                        else maplibreStyleUrl
                    isDefaultStyle.value = !isDefaultStyle.value
                }) {
                Text("Swap style")
            }
        }
    }
}

@Composable
private fun ToggleButton(label: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Button(
        onClick = { onToggle(!enabled) },
        colors = if (enabled) ButtonDefaults.buttonColors()
        else ButtonDefaults.outlinedButtonColors(),
    ) {
        Text(if (enabled) "$label ON" else "$label OFF", fontSize = 11.sp)
    }
}

private fun circlePointsGeoJson(): String = """
{
  "type": "FeatureCollection",
  "features": [
    {"type":"Feature","geometry":{"type":"Point","coordinates":[7.45,46.95]},"properties":{}},
    {"type":"Feature","geometry":{"type":"Point","coordinates":[6.63,46.52]},"properties":{}},
    {"type":"Feature","geometry":{"type":"Point","coordinates":[7.59,47.56]},"properties":{}},
    {"type":"Feature","geometry":{"type":"Point","coordinates":[8.54,47.37]},"properties":{}},
    {"type":"Feature","geometry":{"type":"Point","coordinates":[6.14,46.20]},"properties":{}}
  ]
}
""".trimIndent()

private fun radiusToColor(radius: Float): Int {
    val t = ((radius - 2f) / 38f).coerceIn(0f, 1f)
    val r = (255 * t).toInt()
    val b = (255 * (1 - t)).toInt()
    return (0xFF shl 24) or (r shl 16) or (0 shl 8) or b
}
