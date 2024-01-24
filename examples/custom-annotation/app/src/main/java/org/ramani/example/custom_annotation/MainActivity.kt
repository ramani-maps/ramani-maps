package org.ramani.example.custom_annotation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.mapbox.mapboxsdk.geometry.LatLng
import org.ramani.compose.CameraPosition
import org.ramani.compose.MapLibre
import org.ramani.compose.MapObserver
import org.ramani.example.custom_annotation.ui.theme.CustomAnnotationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomAnnotationTheme {
                val cameraPosition = rememberSaveable {
                    mutableStateOf(
                        CameraPosition(
                            target = LatLng(44.989, 10.809),
                            zoom = 6.0
                        )
                    )
                }
                val progress = remember { mutableFloatStateOf(0.0f) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapLibre(
                        modifier = Modifier.fillMaxSize(),
                        cameraPosition = cameraPosition.value
                    ) {
                        MapObserver(onMapRotated = {
                            progress.floatValue = (it / 360).toFloat()
                        })

                        ProgressCircle(
                            center = LatLng(44.989, 10.809),
                            radius = 25.0f,
                            progress = ProgressPercent((progress.floatValue * 100).toInt()),
                            borderWidth = 5.0f,
                            fillColor = "Orange",
                            borderColor = "Blue",
                            indicatorTextSize = 15.0f,
                        )
                    }
                }
            }
        }
    }
}
