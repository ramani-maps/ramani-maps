package org.maplibre.compose

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    Mapbox.getInstance(context)
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, mapView) {
        val lifecycleObserver = getMapLifecycleObserver(mapView)
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}


fun getMapLifecycleObserver(mapView: MapView): LifecycleEventObserver {
    return LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> throw IllegalStateException()
        }
    }
}

suspend inline fun MapView.awaitMap(): MapboxMap =
    suspendCoroutine { continuation ->
        getMapAsync {
            continuation.resume(it)
        }
    }

class Helper {
    companion object {
        fun getMapTilerKey(context: Context): String? {
            return context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            ).metaData.getString("com.auterion.tazama.mapTilerKey")
        }

        fun validateKey(mapTilerKey: String?) {
            if (mapTilerKey == null) {
                throw Exception("Failed to read MapTiler key")
            }
            if (mapTilerKey.toLowerCase() == "placeholder") {
                throw Exception("Please enter correct MapTiler key in module-level gradle.build file in defaultConfig section")
            }
        }
    }
}