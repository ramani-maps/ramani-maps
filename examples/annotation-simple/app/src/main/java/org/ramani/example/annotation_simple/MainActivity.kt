package org.ramani.example.annotation_simple

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
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
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.ramani.compose.CameraPosition
import org.ramani.compose.Circle
import org.ramani.compose.MapLibre
import org.ramani.compose.Polyline
import org.ramani.compose.Symbol
import org.ramani.example.annotation_simple.ui.theme.AnnotationSimpleTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"
    }

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
                val symbolCenter = rememberSaveable { mutableStateOf(LatLng(4.9, 46.1)) }
                val circleCenter = rememberSaveable { mutableStateOf(LatLng(4.8, 46.0)) }
                val isDefaultStyle = rememberSaveable { mutableStateOf(true) }
                val styleUrl = rememberSaveable { mutableStateOf(DEFAULT_STYLE_URL) }
                // ✅ CORRECT: Remember the styleBuilder to avoid unnecessary recompositions
                val styleBuilder = remember(styleUrl.value) { Style.Builder().fromUri(styleUrl.value) }

                Box {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MapLibre(
                            modifier = Modifier.fillMaxSize(),
                            styleBuilder = styleBuilder,
                            cameraPosition = cameraPosition.value,
                        ) {
                            Symbol(
                                center = symbolCenter.value,
                                isDraggable = true,
                                onSymbolDragged = { center -> symbolCenter.value = center }
                            )
                            Circle(
                                center = circleCenter.value,
                                radius = 50F,
                                isDraggable = true,
                                borderWidth = 2F,
                                onCenterDragged = { center -> circleCenter.value = center }
                            )
                            Polyline(points = polylinePoints, color = "Red", lineWidth = 5.0F)
                        }
                    }
                    Button(
                        modifier = Modifier.align(Alignment.BottomCenter),
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
