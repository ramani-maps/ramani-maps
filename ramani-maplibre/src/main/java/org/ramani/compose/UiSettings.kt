/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2024 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package org.ramani.compose

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.maplibre.android.constants.MapLibreConstants

@Parcelize
class UiSettings(
    val attributionsMargins: Margins = Margins(),
    val compassMargins: Margins = Margins(),
    val deselectMarkersOnTap: Boolean = true,
    val disableRotateWhenScaling: Boolean = true,
    val doubleTapGesturesEnabled: Boolean = true,
    val flingAnimationBaseTime: Long = MapLibreConstants.ANIMATION_DURATION_FLING_BASE,
    val flingThreshold: Long = MapLibreConstants.VELOCITY_THRESHOLD_IGNORE_FLING,
    val flingVelocityAnimationEnabled: Boolean = true,
    val horizontalScrollGesturesEnabled: Boolean = true,
    val increaseScaleThresholdWhenRotating: Boolean = true,
    val isAttributionEnabled: Boolean = true,
    val isLogoEnabled: Boolean = true,
    val logoMargins: Margins = Margins(),
    val quickZoomGesturesEnabled: Boolean = true,
    val rotateGesturesEnabled: Boolean = true,
    val rotateVelocityAnimationEnabled: Boolean = true,
    val scaleVelocityAnimationEnabled: Boolean = true,
    val scrollGesturesEnabled: Boolean = true,
    val tiltGesturesEnabled: Boolean = true,
    val zoomGesturesEnabled: Boolean = true,
    val zoomRate: Float = 1.0f
) : Parcelable {
    constructor(uiSettings: UiSettings) : this(
        attributionsMargins = uiSettings.attributionsMargins,
        compassMargins = uiSettings.compassMargins,
        deselectMarkersOnTap = uiSettings.deselectMarkersOnTap,
        disableRotateWhenScaling = uiSettings.disableRotateWhenScaling,
        doubleTapGesturesEnabled = uiSettings.doubleTapGesturesEnabled,
        flingAnimationBaseTime = uiSettings.flingAnimationBaseTime,
        flingThreshold = uiSettings.flingThreshold,
        flingVelocityAnimationEnabled = uiSettings.flingVelocityAnimationEnabled,
        horizontalScrollGesturesEnabled = uiSettings.horizontalScrollGesturesEnabled,
        increaseScaleThresholdWhenRotating = uiSettings.increaseScaleThresholdWhenRotating,
        isAttributionEnabled = uiSettings.isAttributionEnabled,
        isLogoEnabled = uiSettings.isLogoEnabled,
        logoMargins = uiSettings.logoMargins,
        quickZoomGesturesEnabled = uiSettings.quickZoomGesturesEnabled,
        rotateGesturesEnabled = uiSettings.rotateGesturesEnabled,
        rotateVelocityAnimationEnabled = uiSettings.rotateVelocityAnimationEnabled,
        scaleVelocityAnimationEnabled = uiSettings.scaleVelocityAnimationEnabled,
        scrollGesturesEnabled = uiSettings.scrollGesturesEnabled,
        tiltGesturesEnabled = uiSettings.tiltGesturesEnabled,
        zoomGesturesEnabled = uiSettings.zoomGesturesEnabled,
        zoomRate = uiSettings.zoomRate
    )

    fun copy(
        attributionsMargins: Margins = this.attributionsMargins,
        compassMargins: Margins = this.compassMargins,
        deselectMarkersOnTap: Boolean = this.deselectMarkersOnTap,
        disableRotateWhenScaling: Boolean = this.disableRotateWhenScaling,
        doubleTapGesturesEnabled: Boolean = this.doubleTapGesturesEnabled,
        flingAnimationBaseTime: Long = this.flingAnimationBaseTime,
        flingThreshold: Long = this.flingThreshold,
        flingVelocityAnimationEnabled: Boolean = this.flingVelocityAnimationEnabled,
        horizontalScrollGesturesEnabled: Boolean = this.horizontalScrollGesturesEnabled,
        increaseScaleThresholdWhenRotating: Boolean = this.increaseScaleThresholdWhenRotating,
        isAttributionEnabled: Boolean = this.isAttributionEnabled,
        isLogoEnabled: Boolean = this.isLogoEnabled,
        logoMargins: Margins = this.logoMargins,
        quickZoomGesturesEnabled: Boolean = this.quickZoomGesturesEnabled,
        rotateGesturesEnabled: Boolean = this.rotateGesturesEnabled,
        rotateVelocityAnimationEnabled: Boolean = this.rotateVelocityAnimationEnabled,
        scaleVelocityAnimationEnabled: Boolean = this.scaleVelocityAnimationEnabled,
        scrollGesturesEnabled: Boolean = this.scrollGesturesEnabled,
        tiltGesturesEnabled: Boolean = this.tiltGesturesEnabled,
        zoomGesturesEnabled: Boolean = this.zoomGesturesEnabled,
        zoomRate: Float = this.zoomRate
    ): UiSettings {
        return UiSettings(
            attributionsMargins = attributionsMargins,
            compassMargins = compassMargins,
            deselectMarkersOnTap = deselectMarkersOnTap,
            disableRotateWhenScaling = disableRotateWhenScaling,
            doubleTapGesturesEnabled = doubleTapGesturesEnabled,
            flingAnimationBaseTime = flingAnimationBaseTime,
            flingThreshold = flingThreshold,
            flingVelocityAnimationEnabled = flingVelocityAnimationEnabled,
            horizontalScrollGesturesEnabled = horizontalScrollGesturesEnabled,
            increaseScaleThresholdWhenRotating = increaseScaleThresholdWhenRotating,
            isAttributionEnabled = isAttributionEnabled,
            isLogoEnabled = isLogoEnabled,
            logoMargins = logoMargins,
            quickZoomGesturesEnabled = quickZoomGesturesEnabled,
            rotateGesturesEnabled = rotateGesturesEnabled,
            rotateVelocityAnimationEnabled = rotateVelocityAnimationEnabled,
            scaleVelocityAnimationEnabled = scaleVelocityAnimationEnabled,
            scrollGesturesEnabled = scrollGesturesEnabled,
            tiltGesturesEnabled = tiltGesturesEnabled,
            zoomGesturesEnabled = zoomGesturesEnabled,
            zoomRate = zoomRate
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UiSettings

        return attributionsMargins == other.attributionsMargins &&
                deselectMarkersOnTap == other.deselectMarkersOnTap &&
                disableRotateWhenScaling == other.disableRotateWhenScaling &&
                doubleTapGesturesEnabled == other.doubleTapGesturesEnabled &&
                flingAnimationBaseTime == other.flingAnimationBaseTime &&
                flingThreshold == other.flingThreshold &&
                flingVelocityAnimationEnabled == other.flingVelocityAnimationEnabled &&
                horizontalScrollGesturesEnabled == other.horizontalScrollGesturesEnabled &&
                increaseScaleThresholdWhenRotating == other.increaseScaleThresholdWhenRotating &&
                isAttributionEnabled == other.isAttributionEnabled &&
                isLogoEnabled == other.isLogoEnabled &&
                logoMargins == other.logoMargins &&
                quickZoomGesturesEnabled == other.quickZoomGesturesEnabled &&
                rotateGesturesEnabled == other.rotateGesturesEnabled &&
                rotateVelocityAnimationEnabled == other.rotateVelocityAnimationEnabled &&
                scaleVelocityAnimationEnabled == other.scaleVelocityAnimationEnabled &&
                scrollGesturesEnabled == other.scrollGesturesEnabled &&
                tiltGesturesEnabled == other.tiltGesturesEnabled &&
                zoomGesturesEnabled == other.zoomGesturesEnabled &&
                zoomRate == other.zoomRate &&
                compassMargins == other.compassMargins
    }

    override fun hashCode(): Int {
        var result = attributionsMargins.hashCode()
        result = 31 * result + compassMargins.hashCode()
        result = 31 * result + deselectMarkersOnTap.hashCode()
        result = 31 * result + disableRotateWhenScaling.hashCode()
        result = 31 * result + doubleTapGesturesEnabled.hashCode()
        result = 31 * result + flingAnimationBaseTime.hashCode()
        result = 31 * result + flingThreshold.hashCode()
        result = 31 * result + flingVelocityAnimationEnabled.hashCode()
        result = 31 * result + horizontalScrollGesturesEnabled.hashCode()
        result = 31 * result + increaseScaleThresholdWhenRotating.hashCode()
        result = 31 * result + isAttributionEnabled.hashCode()
        result = 31 * result + isLogoEnabled.hashCode()
        result = 31 * result + logoMargins.hashCode()
        result = 31 * result + quickZoomGesturesEnabled.hashCode()
        result = 31 * result + rotateGesturesEnabled.hashCode()
        result = 31 * result + rotateVelocityAnimationEnabled.hashCode()
        result = 31 * result + scaleVelocityAnimationEnabled.hashCode()
        result = 31 * result + scrollGesturesEnabled.hashCode()
        result = 31 * result + tiltGesturesEnabled.hashCode()
        result = 31 * result + zoomGesturesEnabled.hashCode()
        result = 31 * result + zoomRate.hashCode()
        return result
    }
}

@Parcelize
class Margins(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0) :
    Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Margins

        if (left != other.left) return false
        if (top != other.top) return false
        if (right != other.right) return false
        return bottom == other.bottom
    }

    override fun hashCode(): Int {
        var result = left
        result = 31 * result + top
        result = 31 * result + right
        result = 31 * result + bottom
        return result
    }
}
