package org.ramani.compose

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style

interface CameraPositionCallback {
    fun onChanged(cameraPosition: CameraPosition)
}

interface OnMapReadyCallback {
    fun onMapReady(style: Style)
}

data class GestureContext (
    var coordinate: LatLng? = null
)

interface OnGestureCallback {
    fun onTap(context: GestureContext)
    fun onLongPress(context: GestureContext)
}