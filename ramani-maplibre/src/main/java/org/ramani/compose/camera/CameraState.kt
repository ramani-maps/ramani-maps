package org.ramani.compose.camera

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class CameraState: Parcelable {
    data class Centered(val latitude: Double, val longitude: Double) : CameraState()
    data object TrackingUserLocation : CameraState()
    data object TrackingUserLocationWithHeading : CameraState()
    data object TrackingUserLocationWithCourse : CameraState()
    // TODO: Bounding box & showcase
}