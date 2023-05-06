package org.maplibre.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.mapbox.mapboxsdk.geometry.LatLng

@MapLibreComposable
@Composable
fun SurveyPolygon() {

    val vertice1 = remember {
        mutableStateOf(LatLng(4.91, 46.1))
    }

    val vertice2 = remember {
        mutableStateOf(LatLng(4.91, 47.0))
    }

    val vertice3 = remember {
        mutableStateOf(LatLng(4.63, 47.0))
    }

    val vertice4 = remember {
        mutableStateOf(LatLng(4.63, 46.1))
    }

    Polygon(
        vertices = mutableListOf(
            mutableListOf(
                vertice1.value,
                vertice2.value,
                vertice3.value,
                vertice4.value
            )
        ),
        fillColor = "Green",
        opacity = 0.3f,
        draggable = true,
        onPointsChanged = {
            vertice1.value = it.first().get(0)
            vertice2.value = it.first().get(1)
            vertice3.value = it.first().get(2)
            vertice4.value = it.first().get(3)
        })
    PolyLine(
        points = mutableListOf(
            vertice1.value,
            vertice2.value,
            vertice3.value,
            vertice4.value,
            vertice1.value
        ), color = "Red", lineWidth = 2.0f
    )
    CircleWithItem(
        vertice1.value,
        radius = 8.0f,
        draggable = true,
        color = "Gray",
        borderWidth = 2.0f,
        borderColor = "Black",
        onCenterChanged = { vertice1.value = it })
    CircleWithItem(
        vertice2.value,
        radius = 8.0f,
        draggable = true,
        color = "Gray",
        borderWidth = 2.0f,
        borderColor = "Black",
        onCenterChanged = { vertice2.value = it })
    CircleWithItem(
        vertice3.value,
        radius = 8.0f,
        draggable = true,
        color = "Gray",
        borderWidth = 2.0f,
        borderColor = "Black",
        onCenterChanged = { vertice3.value = it })
    CircleWithItem(
        vertice4.value,
        radius = 8.0f,
        draggable = true,
        color = "Gray",
        borderWidth = 2.0f,
        borderColor = "Black",
        imageSize = 0.7f,
        onCenterChanged = {
            vertice4.value = it
        })
}