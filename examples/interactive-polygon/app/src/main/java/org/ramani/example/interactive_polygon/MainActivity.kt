package org.ramani.example.interactive_polygon

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.maplibre.android.geometry.LatLng
import org.ramani.compose.CameraPosition
import org.ramani.compose.Circle
import org.ramani.compose.MapLibre
import org.ramani.compose.Polygon
import org.ramani.example.interactive_polygon.ui.theme.InteractivePolygonTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InteractivePolygonTheme {
                var polygonCenter = LatLng(44.989, 10.809)
                var polygonState by rememberSaveable { mutableStateOf(polygonPoints) }
                val cameraPosition = rememberSaveable {
                    mutableStateOf(CameraPosition(target = polygonCenter, zoom = 15.0))
                }

                val isDefaultStyle = rememberSaveable { mutableStateOf(true) }
                val styleUrl = rememberSaveable { mutableStateOf(DEFAULT_STYLE_URL) }

                Box {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MapLibre(
                            modifier = Modifier.fillMaxSize(),
                            styleUri = styleUrl.value,
                            cameraPosition = cameraPosition.value,
                        ) {
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
                                draggerImageId = R.drawable.ic_drag,
                                borderWidth = 4.0F,
                                fillColor = "Yellow",
                                opacity = 0.5F,
                                onCenterChanged = { newCenter ->
                                    polygonCenter = newCenter
                                },
                                onVerticesChanged = { newVertices -> polygonState = newVertices },
                            )
                        }
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
                                cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                                    this.target = polygonCenter
                                    this.animationDurationMs = 3000
                                }
                            },
                        ) {
                            Text(text = "Center on polygon")
                        }
                    }
                }
            }
        }
    }

    private val polygonPoints = listOf(
        LatLng(44.986, 10.812),
        LatLng(44.986, 10.807),
        LatLng(44.992, 10.807),
        LatLng(44.992, 10.812),
    )
}
