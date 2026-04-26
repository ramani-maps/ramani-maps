package org.ramani.example.ramani_example

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.ramani.compose.MapLayer
import org.ramani.compose.MapLibre
import org.ramani.compose.MapSource
import java.net.URI

@Composable
fun ClustersScreen() {
    val pointCount = Expression.toNumber(Expression.get("point_count"))

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        MapLibre(
            modifier = Modifier.fillMaxSize(),
        ) {
            MapSource {
                GeoJsonSource(
                    "earthquakes",
                    URI("https://www.mapbox.com/mapbox-gl-js/assets/earthquakes.geojson"),
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50),
                )
            }
            MapLayer {
                CircleLayer("unclustered", "earthquakes").apply {
                    setFilter(Expression.has("mag"))
                    setProperties(
                        PropertyFactory.circleRadius(10F),
                        PropertyFactory.circleColor(
                            Expression.interpolate(
                                Expression.exponential(1),
                                Expression.get("mag"),
                                Expression.stop(2.0, Expression.rgb(0, 255, 0)),
                                Expression.stop(4.5, Expression.rgb(0, 0, 255)),
                                Expression.stop(7.0, Expression.rgb(255, 0, 0)),
                            )
                        ),
                    )
                }
            }
            MapLayer {
                CircleLayer("cluster", "earthquakes").apply {
                    setFilter(Expression.gt(pointCount, 1))
                    setProperties(
                        PropertyFactory.circleColor(Color.BLACK),
                        PropertyFactory.circleRadius(18F),
                    )
                }
            }
            MapLayer {
                SymbolLayer("count", "earthquakes").apply {
                    setFilter(Expression.gt(pointCount, 1))
                    setProperties(
                        PropertyFactory.textField(Expression.toString(pointCount)),
                        PropertyFactory.textSize(12F),
                        PropertyFactory.textColor(Color.WHITE),
                        PropertyFactory.textIgnorePlacement(true),
                        PropertyFactory.textAllowOverlap(true),
                    )
                }
            }
        }
    }
}
