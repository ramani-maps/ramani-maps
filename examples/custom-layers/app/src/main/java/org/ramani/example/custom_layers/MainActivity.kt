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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.VectorSource
import org.ramani.compose.CameraPosition
import org.ramani.compose.Circle
import org.ramani.compose.MapLibre
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

        // This is usually done in the MapLibre composable, but in this case we need to initialize
        // the map earlier in order to define the sources and layers.
        MapLibre.getInstance(this)

        // Define the source GeoJson:
        //    "Geographical UAS zones of Switzerland"
        //    Federal Office of Civil Aviation (FOCA)
        //    https://opendata.swiss/en/dataset/geografische-uas-gebiete-der-schweiz
        val nfzSource = GeoJsonSource(
            "no-fly-zones",
            URI("https://data.geo.admin.ch/ch.bazl.einschraenkungen-drohnen/einschraenkungen-drohnen/einschraenkungen-drohnen_4326.geojson"),
        )

        val nfzPoly = LineLayer("nfz-poly", "no-fly-zones")
        nfzPoly.setProperties(
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

        val nfzFill = FillLayer("nfz-fill", "no-fly-zones")
        nfzFill.setProperties(
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

        // Show the contour lines coming from thunderforest.com
        val thunder_key = resources.getString(R.string.thunderforest_api_key)
        val contourSource = VectorSource(
            "contours",
            "https://tile.thunderforest.com/thunderforest.outdoors-v2.json?apikey=$thunder_key",
        )

        val contourLayer = LineLayer("contour", "contours")
        contourLayer.sourceLayer = "elevation"

        // Show the hillshades coming from maptiler.com
        val maptiler_key = resources.getString(R.string.maptiler_api_key)
        val hillShadeSource = RasterSource(
            "hillshades",
            "https://api.maptiler.com/tiles/hillshade/tiles.json?key=$maptiler_key",
        )

        val hillshadeLayer = RasterLayer("hillshades-layer", "hillshades")

        setContent {
            val cameraPosition = rememberSaveable {
                mutableStateOf(
                    CameraPosition(target = LatLng(46.6, 7.1), zoom = 8.0)
                )
            }
            val isDefaultStyle = rememberSaveable { mutableStateOf(true) }
            val styleUrl = rememberSaveable { mutableStateOf(DEFAULT_STYLE_URL) }
            // ✅ CORRECT: Remember the styleBuilder to avoid unnecessary recompositions
            val styleBuilder = remember(styleUrl.value) { Style.Builder().fromUri(styleUrl.value) }
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
                            styleBuilder = styleBuilder,
                            uiSettings = uiSettings.value,
                            cameraPosition = cameraPosition.value,
                            sources = listOf(
                                nfzSource,
                                contourSource,
                                hillShadeSource,
                            ),
                            layers = listOf(
                                nfzFill,
                                nfzPoly,
                                hillshadeLayer,
                                contourLayer,
                            ),
                        ) {
                            Symbol(center = LatLng(46.5, 6.4))
                            Circle(
                                center = LatLng(46.5, 6.4),
                                radius = 40F,
                                isDraggable = true,
                            )
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
