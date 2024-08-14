package org.ramani.example.annotation_simple

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mapbox.android.gestures.StandardScaleGestureDetector
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap.OnScaleListener
import org.ramani.compose.CameraPosition
import org.ramani.compose.Circle
import org.ramani.compose.DisposableMapEffect
import org.ramani.compose.MapEffect
import org.ramani.compose.MapLibre
import org.ramani.compose.Polyline
import org.ramani.example.annotation_simple.ui.theme.AnnotationSimpleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AnnotationSimpleTheme {
                val cameraPosition = rememberSaveable {
                    mutableStateOf(
                        CameraPosition(
                            target = LatLng(46.0, 4.8),
                            zoom = 2.0,
                        )
                    )
                }
                val circleCenter = rememberSaveable { mutableStateOf(LatLng(4.8, 46.0)) }

                val scaleValue = rememberSaveable { mutableStateOf("") }

                val scaleListener = object : OnScaleListener {
                    override fun onScaleBegin(p0: StandardScaleGestureDetector) {
                        scaleValue.value = "onScaleBegin"
                    }

                    override fun onScale(p0: StandardScaleGestureDetector) {
                        scaleValue.value = "onScale"
                    }

                    override fun onScaleEnd(p0: StandardScaleGestureDetector) {
                        scaleValue.value = "onScaleEnd"
                    }

                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MapLibre(
                            modifier = Modifier.fillMaxSize(),
                            styleUrl = resources.getString(R.string.maplibre_style_url),
                            cameraPosition = cameraPosition.value,
                        ) {
                            Circle(
                                center = circleCenter.value,
                                radius = 50F,
                                isDraggable = true,
                                borderWidth = 2F,
                                onCenterDragged = { center -> circleCenter.value = center }
                            )
                            Polyline(points = polylinePoints, color = "Red", lineWidth = 5.0F)

                            MapEffect(key1 = Unit) {
                                it.getMapAsync { mapLibre ->
                                    mapLibre.setMaxZoomPreference(21.0)
                                    mapLibre.setMinZoomPreference(5.0)
                                }
                            }

                            DisposableMapEffect(key1 = Unit) {
                                it.getMapAsync { mapLibre ->
                                    mapLibre.addOnScaleListener(scaleListener)
                                }
                                onDispose {
                                    it.getMapAsync { mapLibre ->
                                        mapLibre.removeOnScaleListener(scaleListener)
                                    }
                                }
                            }
                        }

                        Text(
                            "Scale status = ${scaleValue.value}",
                            style = TextStyle(color = Color.Black),
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        )
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
