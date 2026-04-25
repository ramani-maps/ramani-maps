package org.ramani.example.interactive_polygon

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.maplibre.android.geometry.LatLng
import org.ramani.compose.CameraPosition
import org.ramani.compose.CenterState
import org.ramani.compose.Circle
import org.ramani.compose.MapLibre
import org.ramani.compose.MapStyle
import org.ramani.compose.Polygon
import org.ramani.compose.rememberCameraPositionState

private const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"

private val polygonPoints = listOf(
    LatLng(44.986, 10.812),
    LatLng(44.986, 10.807),
    LatLng(44.992, 10.807),
    LatLng(44.992, 10.812),
)

@Composable
fun InteractivePolygonScreen() {
    val context = LocalContext.current
    var polygonCenter = LatLng(44.989, 10.809)
    val vertexStates = remember {
        polygonPoints.map { CenterState(it) }
    }
    val cameraPositionState = rememberCameraPositionState(
        CameraPosition(target = polygonCenter, zoom = 15.0)
    )

    val isDefaultStyle = rememberSaveable { mutableStateOf(true) }
    val styleUrl = rememberSaveable { mutableStateOf(DEFAULT_STYLE_URL) }
    val style = MapStyle.Uri(styleUrl.value)

    Box {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MapLibre(
                modifier = Modifier.fillMaxSize(),
                style = style,
                cameraPositionState = cameraPositionState,
            ) {
                vertexStates.forEach { state ->
                    Circle(
                        centerState = state,
                        aboveLayerId = "editable_polygon",
                        radius = 10.0F,
                        color = "Blue",
                        isDraggable = true,
                    )
                }
                Polygon(
                    layerId = "editable_polygon",
                    vertices = vertexStates.map { it.center },
                    isDraggable = true,
                    draggerImageId = R.drawable.ic_drag,
                    borderWidth = 4.0F,
                    fillColor = "Yellow",
                    opacity = 0.5F,
                    onCenterChanged = { newCenter ->
                        polygonCenter = newCenter
                    },
                    onVerticesChanged = { newVertices ->
                        newVertices.forEachIndexed { index, vertex ->
                            vertexStates[index].center = vertex
                        }
                    },
                )
            }
        }
        val camPos = cameraPositionState.position
        Text(
            text = "lat: %.4f  lng: %.4f  zoom: %.1f".format(
                camPos.target?.latitude,
                camPos.target?.longitude,
                camPos.zoom
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    styleUrl.value =
                        if (!isDefaultStyle.value) DEFAULT_STYLE_URL
                        else context.getString(R.string.maplibre_style_url)
                    isDefaultStyle.value = !isDefaultStyle.value
                }) {
                Text("Swap style")
            }
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    cameraPositionState.position = cameraPositionState.position.copy(
                        target = polygonCenter,
                        animationDurationMs = 3000,
                    )
                },
            ) {
                Text(text = "Center on polygon")
            }
        }
    }
}
