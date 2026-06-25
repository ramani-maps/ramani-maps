package org.ramani.compose

import MapLibre.MLNSettings
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle

@OptIn(ExperimentalForeignApi::class)
internal object MapLibreInitializer {
    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        val resourcePath = NSBundle.mainBundle.resourcePath ?: return
        val bundlePath = "$resourcePath/compose-resources/composeResources" +
            "/org.ramani_maps.ramani_maplibre.generated.resources/files"
        val bundle = NSBundle.bundleWithPath(bundlePath)
        if (bundle != null) {
            MLNSettings.setCustomResourceBundle(bundle)
        }
    }
}
