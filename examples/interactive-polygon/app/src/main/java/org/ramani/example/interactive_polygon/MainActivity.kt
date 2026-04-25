package org.ramani.example.interactive_polygon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.maplibre.android.MapLibre
import org.ramani.example.interactive_polygon.ui.theme.InteractivePolygonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        setContent {
            InteractivePolygonTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { ExampleListScreen(navController) }
                    composable("annotation") { AnnotationScreen() }
                    composable("clusters") { ClustersScreen() }
                    composable("custom-annotation") { CustomAnnotationScreen() }
                    composable("custom-layers") { CustomLayersScreen() }
                    composable("interactive-polygon") { InteractivePolygonScreen() }
                    composable("location") { LocationScreen() }
                }
            }
        }
    }
}
