package org.ramani.compose.camera

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// TODO: Discuss this being operated in the camera, not map properties.
@Parcelize
sealed class CameraPitch : Parcelable {
    object Free: CameraPitch()
    data class FreeWithPitch(val minimum: Double, val maximum: Double): CameraPitch()
    data class Fixed(val pitch: Double): CameraPitch()

    val rangeValue: Pair<Double, Double>
        get() = when (this) {
            is Free -> Pair(0.0, 60.0)
            is FreeWithPitch -> Pair(minimum, maximum)
            is Fixed -> Pair(pitch, pitch)
        }
}