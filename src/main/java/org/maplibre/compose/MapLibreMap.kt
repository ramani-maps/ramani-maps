package org.maplibre.compose


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView

@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "Maplibre Composable")
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class MapLibreComposable

@Composable
fun MapLibre(
    modifier: Modifier,
    content: (@Composable @MapLibreComposable () -> Unit)? = null,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    LocalContext.current
    val map = rememberMapViewWithLifecycle()

    val currentContent by rememberUpdatedState(content)
    val parentComposition = rememberCompositionContext()

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { map })
    LaunchedEffect(Unit) {
        disposingComposition {
            map.newComposition(parentComposition, style = map.awaitMap().awaitStyle()) {
                CompositionLocalProvider() {
                    currentContent?.invoke()
                }
            }
        }

    }
}