package org.ramani.example.custom_layers

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.VectorSource
import org.ramani.compose.CameraPosition
import org.ramani.compose.rememberCameraPositionState
import org.ramani.compose.Circle
import org.ramani.compose.MapLayer
import org.ramani.compose.MapLibre
import org.ramani.compose.MapSource
import org.ramani.compose.MapStyle
import org.ramani.compose.Symbol
import org.ramani.compose.UiSettings
import org.ramani.example.custom_layers.ui.theme.CustomLayersTheme
import java.net.URI

class MainActivity : ComponentActivity() {
    companion object {
        const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        val thunderKey = resources.getString(R.string.thunderforest_api_key)
        val maptilerKey = resources.getString(R.string.maptiler_api_key)

        setContent {
            val cameraPositionState = rememberCameraPositionState(
                CameraPosition(target = LatLng(46.6, 7.1), zoom = 8.0)
            )
            val isDefaultStyle = rememberSaveable { mutableStateOf(true) }
            val styleUrl = rememberSaveable { mutableStateOf(DEFAULT_STYLE_URL) }
            val uiSettings = rememberSaveable {
                mutableStateOf(
                    UiSettings(rotateGesturesEnabled = false)
                )
            }

            CustomLayersTheme {
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
                            MapSource {
                                GeoJsonSource(
                                    "no-fly-zones",
                                    URI("https://data.geo.admin.ch/ch.bazl.einschraenkungen-drohnen/einschraenkungen-drohnen/einschraenkungen-drohnen_4326.geojson"),
                                )
                            }
                            MapSource {
                                VectorSource(
                                    "contours",
                                    "https://tile.thunderforest.com/thunderforest.outdoors-v2.json?apikey=$thunderKey",
                                )
                            }
                            MapSource {
                                RasterSource(
                                    "hillshades",
                                    "https://api.maptiler.com/tiles/hillshade/tiles.json?key=$maptilerKey",
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
                            MapLayer {
                                RasterLayer("hillshades-layer", "hillshades")
                            }
                            MapLayer {
                                LineLayer("contour", "contours").apply {
                                    sourceLayer = "elevation"
                                }
                            }
                            Circle(
                                center = LatLng(46.5, 6.4),
                                radius = 40F,
                                isDraggable = true,
                            )
                            Symbol(center = LatLng(46.5, 6.4))
                        }
                    }
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
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
                                    else resources.getString(R.string.maplibre_style_url)

                                isDefaultStyle.value = !isDefaultStyle.value
                            }) {
                            Text("Swap style")
                        }
                    }
                }
            }
        }
    }
}
