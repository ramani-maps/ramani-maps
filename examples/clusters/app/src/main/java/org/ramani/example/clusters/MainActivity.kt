package org.ramani.example.clusters

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import org.ramani.compose.MapLibre
import org.ramani.example.clusters.ui.theme.ClustersTheme
import java.net.URI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This is usually done in the MapLibre composable, but in this case we need to initialize
        // the map earlier in order to define the sources and layers.
        Mapbox.getInstance(this)

        // Define the source GeoJson.
        val mySource = GeoJsonSource(
            "earthquakes",
            URI("https://www.mapbox.com/mapbox-gl-js/assets/earthquakes.geojson"),
            GeoJsonOptions()
                .withCluster(true)
                .withClusterMaxZoom(14)
                .withClusterRadius(50),
        )

        // Define a layer made of all the data points from the "earthquakes" source
        // that have a "mag" property. Also set the color of the data points based on "mag".
        val unclustered = CircleLayer("unclustered", "earthquakes")
        unclustered.setFilter(Expression.has("mag"))
        unclustered.setProperties(
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

        // Maplibre will create cluster data points automatically. Those points can be identified
        // from their "point_count" property, which says how many points the cluster contains.
        // We extract that value here to use it for the cluster layers below.
        val pointCount = Expression.toNumber(Expression.get("point_count"))

        // Create a layer made of the cluster points that were created for the "earthquakes" source.
        // Note how we select the points that have "point_cloud > 1", which are clusters.
        val clusters = CircleLayer("cluster", "earthquakes")
        clusters.setFilter(Expression.gt(pointCount, 1))
        clusters.setProperties(
            PropertyFactory.circleColor(Color.BLACK),
            PropertyFactory.circleRadius(18F),
        )

        // Create another layer, this time to show the "point_count" on top of the cluster circles
        // defined in the "clusters" layer above.
        val numbers = SymbolLayer("count", "earthquakes")
        numbers.setFilter(Expression.gt(pointCount, 1))
        numbers.setProperties(
            PropertyFactory.textField(Expression.toString(pointCount)),
            PropertyFactory.textSize(12F),
            PropertyFactory.textColor(Color.WHITE),
            PropertyFactory.textIgnorePlacement(true),
            PropertyFactory.textAllowOverlap(true),
        )

        setContent {
            ClustersTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapLibre(
                        modifier = Modifier.fillMaxSize(),
                        sources = listOf(mySource),
                        layers = listOf(unclustered, clusters, numbers),
                    )
                }
            }
        }
    }
}
