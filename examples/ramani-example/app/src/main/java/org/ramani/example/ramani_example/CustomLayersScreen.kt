package org.ramani.example.ramani_example

import android.graphics.Color
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.VectorSource
import org.ramani.compose.CameraPosition
import org.ramani.compose.MapLayer
import org.ramani.compose.MapLibre
import org.ramani.compose.MapSource
import org.ramani.compose.MapStyle
import org.ramani.compose.UiSettings
import org.ramani.compose.rememberCameraPositionState
import java.net.URI
import androidx.compose.ui.res.stringResource

private const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"

@Composable
fun CustomLayersScreen() {
    val maplibreStyleUrl = stringResource(R.string.maplibre_style_url)
    val thunderKey = stringResource(R.string.thunderforest_api_key)
    val maptilerKey = stringResource(R.string.maptiler_api_key)

    val cameraPositionState = rememberCameraPositionState(
        CameraPosition(target = LatLng(46.6, 7.1), zoom = 8.0)
    )
    val isDefaultStyle = rememberSaveable { mutableStateOf(true) }
    val styleUrl = rememberSaveable { mutableStateOf(DEFAULT_STYLE_URL) }
    val uiSettings = rememberSaveable {
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
                    MapSource {
                        GeoJsonSource(
                            "no-fly-zones",
                            URI("https://data.geo.admin.ch/ch.bazl.einschraenkungen-drohnen/einschraenkungen-drohnen/einschraenkungen-drohnen_4326.geojson"),
                        )
                    }
                    MapLayer {
                        FillLayer("nfz-fill", "no-fly-zones").apply {
                            setProperties(
                                PropertyFactory.fillColor(
                                    Expression.switchCase(
                                        Expression.has("fill"),
                                        Expression.get("fill"),
                                        Expression.color(Color.BLUE),
                                    )
                                ),
                                PropertyFactory.fillOpacity(
                                    Expression.switchCase(
                                        Expression.has("fill-opacity"),
                                        Expression.get("fill-opacity"),
                                        Expression.literal(0.4F),
                                    )
                                ),
                            )
                        }
                    }
                    MapLayer {
                        LineLayer("nfz-poly", "no-fly-zones").apply {
                            setProperties(
                                PropertyFactory.lineColor(
                                    Expression.switchCase(
                                        Expression.has("stroke"),
                                        Expression.get("stroke"),
                                        Expression.color(Color.BLUE),
                                    )
                                ),
                                PropertyFactory.lineWidth(
                                    Expression.switchCase(
                                        Expression.has("stroke-width"),
                                        Expression.get("stroke-width"),
                                        Expression.literal(2),
                                    )
                                ),
                                PropertyFactory.lineOpacity(
                                    Expression.switchCase(
                                        Expression.has("stroke-opacity"),
                                        Expression.get("stroke-opacity"),
                                        Expression.literal(0.4F),
                                    )
                                ),
                            )
                        }
                    }
                }

                if (showHillshades && maptilerKey.isNotEmpty()) {
                    MapSource {
                        RasterSource(
                            "hillshades",
                            "https://api.maptiler.com/tiles/hillshade/tiles.json?key=$maptilerKey",
                        )
                    }
                    MapLayer {
                        RasterLayer("hillshades-layer", "hillshades")
                    }
                }

                if (showContours && thunderKey.isNotEmpty()) {
                    MapSource {
                        VectorSource(
                            "contours",
                            "https://tile.thunderforest.com/thunderforest.outdoors-v2.json?apikey=$thunderKey",
                        )
                    }
                    MapLayer {
                        LineLayer("contour", "contours").apply {
                            sourceLayer = "elevation"
                        }
                    }
                }

                MapSource {
                    GeoJsonSource("circle-points").apply {
                        setGeoJson(circlePointsGeoJson())
                    }
                }
                MapLayer(
                    update = { layer ->
                        layer.setProperties(
                            PropertyFactory.circleRadius(circleRadius),
                            PropertyFactory.circleColor(radiusToColor(circleRadius)),
                            PropertyFactory.circleOpacity(0.7f),
                        )
                    },
                    keys = listOf(circleRadius),
                ) {
                    CircleLayer("circle-points-layer", "circle-points").apply {
                        setProperties(
                            PropertyFactory.circleRadius(circleRadius),
                            PropertyFactory.circleColor(radiusToColor(circleRadius)),
                            PropertyFactory.circleOpacity(0.7f),
                        )
                    }
                }
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
    return Color.rgb(r, 0, b)
}
