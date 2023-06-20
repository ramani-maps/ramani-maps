# Ramani-Maps

An Android Compose library to manipulate maps. We currently support MapLibre,
but we started looking into Mapbox already!

## What does the license say?

Ramani-Maps is licensed under the [Mozilla Public License v2.0](https://www.mozilla.org/en-US/MPL/2.0/).
A very good resource to understand it is the [MPL-2.0 FAQ](https://www.mozilla.org/en-US/MPL/2.0/FAQ/),
but the idea is this:

* If you only use the library (without modifying its source code), then you
  should just add a link to this repo somewhere in your app.
* If you modify the library (e.g. by fixing a bug), then you should make the
  changes available to your users, e.g. by maintaining a public fork, or by
  contributing the fixes to this repo.

## Quick Start with MapLibre

Add the dependency to `build.gradle`:

```gradle
implementation 'org.ramani-maps:ramani-maplibre:0.1.0'
```

Insert the map composable:

```kotlin
MapLibre(modifier = Modifier.fillMaxSize(), apiKey = "<your API key here>")
```

The map will now appear in your app!

## Temporary fork

In order for our more advanced interactive annotations to work, we had to
modify the [MapLibre "annotation" plugin](https://github.com/maplibre/maplibre-plugins-android).
We are working towards getting our changes upstream, but this takes time.
In the meantime, we advise you use our fork as a Gradle "composite build",
by doing the following:

1. Clone https://github.com/ramani-maps/maplibre-plugins-android somewhere
2. Add the following to your `settings.gradle`:

```
includeBuild '/path/to/maplibre-plugins-android/plugin-annotation'
```

## Examples

We provide a few simple examples demonstrating some of the supported features.

### Interactive Polygon

![interactive polygon example](./docs/interactive-polygon-example.gif)

The complete application is available in [examples/interactive-polygon](./examples/interactive-polygon),
but the actual code is extremely short:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InteractivePolygonTheme {
                var polygonState by remember { mutableStateOf(polygonPoints) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Create the map
                    MapLibre(modifier = Modifier.fillMaxSize(), apiKey = "<your API key here>") {
                        // Create a handle for each vertex (those are blue circles)
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
                        // Draw the polygon
                        Polygon(
                            vertices = polygonState,
                            isDraggable = true,
                            draggerImageId = R.drawable.ic_drag,
                            borderWidth = 4.0F,
                            fillColor = "Yellow",
                            opacity = 0.5F,
                            onVerticesChanged = { newVertices -> polygonState = newVertices },
                        )
                    }
                }
            }
        }
    }

    // Initial position of the polygon
    private val polygonPoints = listOf(
        LatLng(54.9, 0.8),
        LatLng(54.9, 46.2),
        LatLng(20.8, 46.2),
        LatLng(20.8, 0.8),
    )
}
```

### Annotation Simple

![annotation simple example](./docs/annotation-simple-example.gif)

The complete application is available in [examples/annotation-simple](./examples/annotation-simple),
but the actual code is extremely short:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnnotationSimpleTheme {
                val polylineState by remember { mutableStateOf(polylinePoints) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Create the map
                    MapLibre(modifier = Modifier.fillMaxSize(), apiKey = "2z0TwvuXjwgOpvle5GYY") {
                        // Create the draggable circle
                        Circle(
                            center = LatLng(4.8, 46.0),
                            radius = 50F,
                            isDraggable = true,
                            borderWidth = 2F,
                        )
                        // Create the polyline
                        Polyline(points = polylineState, color = "Red", lineWidth = 5.0F)
                    }
                }
            }
        }
    }

    // Coordinates of the polyline
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
```

## Contributions

Contributions are of course very welcome and we are happy to assist
getting pull requests reviewed and merged.
Please note that the library currently only exposes a small subset
of the underlying map functionality and so there are plenty of features
that can still be added.

