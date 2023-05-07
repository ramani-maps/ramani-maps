package org.maplibre.compose

import androidx.compose.runtime.Composable
import com.mapbox.mapboxsdk.geometry.LatLng


data class Vertice(
    val location: LatLng,
    val radius: Float,
    val draggable: Boolean = false,
    val color: String = "Black"
)


@MapLibreComposable
@Composable
fun SurveyPolygon(
    vertices: MutableList<Vertice>,
    onVerticesChanged: (MutableList<Vertice>) -> Unit,
    onVerticeAtIndexChanged: (Int, Vertice) -> Unit
) {

    val pointsForPolyline: MutableList<LatLng> = vertices.map { it.location }.toMutableList()
    pointsForPolyline.add(vertices.first().location)

    Polygon(
        vertices = mutableListOf(
            vertices.map { it.location }.toMutableList()
        ),
        fillColor = "Green",
        opacity = 0.3f,
        draggable = true,
        onVerticesChanged = {
            onVerticesChanged(it.first().mapIndexed { index, latLng ->
                vertices[index].copy(location = latLng)
            }.toMutableList())
        })
    PolyLine(
        points = pointsForPolyline,
        color = "Black",
        lineWidth = 2.0f
    )

    vertices.forEachIndexed { index, vertice ->
        CircleWithItem(
            center = vertice.location,
            radius = vertice.radius,
            draggable = vertice.draggable,
            color = vertice.color,
            onCenterChanged = { latLng ->
                onVerticeAtIndexChanged(index, vertice.copy(location = latLng))
            }
        )
    }
}