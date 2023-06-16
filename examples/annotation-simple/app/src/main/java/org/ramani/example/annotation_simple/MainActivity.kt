package org.ramani.example.annotation_simple

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.geometry.LatLng
import org.ramani.compose.Circle
import org.ramani.compose.MapLibre
import org.ramani.compose.Polyline
import org.ramani.example.annotation_simple.ui.theme.AnnotationSimpleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnnotationSimpleTheme {
                val polylineState by remember { mutableStateOf(polylinePoints.toMutableList()) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapLibre(modifier = Modifier.fillMaxSize(), apiKey = "2z0TwvuXjwgOpvle5GYY") {
                        Circle(
                            center = LatLng(4.8, 46.0),
                            radius = 50F,
                            isDraggable = true,
                            borderWidth = 2F,
                        )
                        Polyline(points = polylineState, color = "Red", lineWidth = 5.0F)
                    }
                }
            }
        }
    }

    private val polylinePoints = listOf(
        LatLng(34.9, 46.2),
        LatLng(64.0, 0.8),
        LatLng(46.2, -46.5),
        LatLng(54.1, -86.2),
        LatLng(24.9, -116.6),
        LatLng(0.0, -171.3),
        LatLng(34.9, -240.1),
        LatLng(34.9, -313.8),
    )
}
