package org.ramani.example.interactive_polygon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.geometry.LatLng
import org.ramani.compose.Circle
import org.ramani.compose.MapLibre
import org.ramani.compose.Polygon
import org.ramani.example.interactive_polygon.ui.theme.InteractivePolygonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InteractivePolygonTheme {
                var polygonState by remember { mutableStateOf(polygonPoints) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapLibre(modifier = Modifier.fillMaxSize(), apiKey = "2z0TwvuXjwgOpvle5GYY") {
                        polygonState.forEachIndexed { index, vertex ->
                            Circle(
                                center = vertex,
                                radius = 10.0F,
                                color = "Blue",
                                zIndex = 1,
                                isDraggable = true,
                                onCenterDragged = { newCenter ->
                                    polygonState = polygonState.toMutableList()
                                        .apply { this[index] = newCenter }
                                }
                            )
                        }
                        Polygon(
                            vertices = polygonState,
                            isDraggable = true,
                            draggerImageId = com.mapbox.mapboxsdk.R.drawable.maplibre_compass_icon,
                            borderWidth = 4.0F,
                            fillColor = "Yellow",
                            opacity = 0.5F,
                            onVerticesChanged = { newVertices -> polygonState = newVertices },
                        )
                    }
                }
            }
        }
    }

    private val polygonPoints = listOf(
        LatLng(54.9, 0.8),
        LatLng(54.9, 46.2),
        LatLng(20.8, 46.2),
        LatLng(20.8, 0.8),
    )
}
