package org.ramani.example.annotation_simple

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.android.geometry.LatLng
import org.ramani.compose.CameraPosition
import org.ramani.compose.rememberCameraPositionState
import org.ramani.compose.MapStyle
import org.ramani.compose.Circle
import org.ramani.compose.CenterState
import org.ramani.compose.MapLibre
import org.ramani.compose.Polyline
import org.ramani.compose.Symbol
import org.ramani.example.annotation_simple.ui.theme.AnnotationSimpleTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"
        val INITIAL_CIRCLE_CENTER = LatLng(4.8, 46.0)
        val INITIAL_SYMBOL_CENTER = LatLng(4.9, 46.1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AnnotationSimpleTheme {
                val cameraPositionState = rememberCameraPositionState(
                    CameraPosition(
                        target = LatLng(46.0, 4.8),
                        zoom = 2.0,
                    )
                )
                val symbolCenterState = rememberSaveable(saver = CenterState.Saver) {
                    CenterState(INITIAL_SYMBOL_CENTER)
                }
                val circleCenterState = rememberSaveable(saver = CenterState.Saver) {
                    CenterState(INITIAL_CIRCLE_CENTER)
                }
                val isDefaultStyle = rememberSaveable { mutableStateOf(true) }
                val styleUrl = rememberSaveable { mutableStateOf(DEFAULT_STYLE_URL) }
                val mapStyle = MapStyle.Uri(styleUrl.value)

                Box {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MapLibre(
                            modifier = Modifier.fillMaxSize(),
                            style = mapStyle,
                            cameraPositionState = cameraPositionState,
                        ) {
                            Circle(
                                centerState = circleCenterState,
                                radius = 50F,
                                isDraggable = true,
                                borderWidth = 2F,
                            )
                            Polyline(points = polylinePoints, color = "Red", lineWidth = 5.0F)
                            Symbol(
                                centerState = symbolCenterState,
                                isDraggable = true,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 48.dp)
                            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "Circle: lat: %.4f  lng: %.4f".format(
                                circleCenterState.center.latitude,
                                circleCenterState.center.longitude,
                            ),
                        )
                        Text(
                            text = "Symbol: lat: %.4f  lng: %.4f".format(
                                symbolCenterState.center.latitude,
                                symbolCenterState.center.longitude,
                            ),
                        )
                    }
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
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
                        Button(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            onClick = {
                                circleCenterState.center = INITIAL_CIRCLE_CENTER
                                symbolCenterState.center = INITIAL_SYMBOL_CENTER
                            },
                        ) {
                            Text("Reset annotations")
                        }
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
