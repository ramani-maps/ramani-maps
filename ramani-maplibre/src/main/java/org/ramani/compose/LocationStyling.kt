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

import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize

/**
 * @property accuracyAlpha Opacity of the accuracy view between 0 (transparent) and 1 (opaque).
 * @property accuracyColor Color of the accuracy view.
 * @property enablePulse Enable the location pulsing circle.
 * @property enablePulseFade Enable the fading of the pulsing circle.
 * @property pulseColor Color of the pulsing circle.
 * @property bearingTintColor Defines the bearing icon color as an integer (AARRGGBB).
 * @property foregroundTintColor Defines the foreground color as an integer (AARRGGBB).
 * @property backgroundTintColor Defines the background color as an integer (AARRGGBB).
 * @property foregroundStaleTintColor Defines the foreground stale color as an integer (AARRGGBB).
 * @property backgroundStaleTintColor Defines the background stale color as an integer (AARRGGBB).
 */
@Parcelize
class LocationStyling(
    var accuracyAlpha: Float? = null,
    @ColorInt var accuracyColor: Int? = null,
    var enablePulse: Boolean? = null,
    var enablePulseFade: Boolean? = null,
    @ColorInt var pulseColor: Int? = null,
    @ColorInt var bearingTintColor: Int? = null,
    @ColorInt var foregroundTintColor: Int? = null,
    @ColorInt var backgroundTintColor: Int? = null,
    @ColorInt var foregroundStaleTintColor: Int? = null,
    @ColorInt var backgroundStaleTintColor: Int? = null,
) : Parcelable {
    constructor(locationStyling: LocationStyling) : this(
        locationStyling.accuracyAlpha,
        locationStyling.accuracyColor,
        locationStyling.enablePulse,
        locationStyling.enablePulseFade,
        locationStyling.pulseColor,
        locationStyling.bearingTintColor,
        locationStyling.foregroundTintColor,
        locationStyling.backgroundTintColor,
        locationStyling.foregroundStaleTintColor,
        locationStyling.backgroundStaleTintColor,
    )

    fun copy(
        accuracyAlpha: Float? = this.accuracyAlpha,
        @ColorInt accuracyColor: Int? = this.accuracyColor,
        enablePulse: Boolean? = this.enablePulse,
        enablePulseFade: Boolean? = this.enablePulseFade,
        @ColorInt pulseColor: Int? = this.pulseColor,
        @ColorInt bearingTintColor: Int? = this.bearingTintColor,
        @ColorInt foregroundTintColor: Int? = this.foregroundTintColor,
        @ColorInt backgroundTintColor: Int? = this.backgroundTintColor,
        @ColorInt foregroundStaleTintColor: Int? = this.foregroundStaleTintColor,
        @ColorInt backgroundStaleTintColor: Int? = this.backgroundStaleTintColor,
    ): LocationStyling {
        return LocationStyling(
            accuracyAlpha = accuracyAlpha,
            accuracyColor = accuracyColor,
            enablePulse = enablePulse,
            enablePulseFade = enablePulseFade,
            pulseColor = pulseColor,
            bearingTintColor = bearingTintColor,
            foregroundTintColor = foregroundTintColor,
            backgroundTintColor = backgroundTintColor,
            foregroundStaleTintColor = foregroundStaleTintColor,
            backgroundStaleTintColor = backgroundStaleTintColor,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocationStyling

        if (accuracyAlpha != other.accuracyAlpha) return false
        if (accuracyColor != other.accuracyColor) return false
        if (enablePulse != other.enablePulse) return false
        if (enablePulseFade != other.enablePulseFade) return false
        if (pulseColor != other.pulseColor) return false
        if (bearingTintColor != other.bearingTintColor) return false
        if (foregroundTintColor != other.foregroundTintColor) return false
        if (backgroundTintColor != other.backgroundTintColor) return false
        if (foregroundStaleTintColor != other.foregroundStaleTintColor) return false
        if (backgroundStaleTintColor != other.backgroundStaleTintColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = accuracyAlpha?.hashCode() ?: 0
        result = 31 * result + (accuracyColor ?: 0)
        result = 31 * result + (enablePulse?.hashCode() ?: 0)
        result = 31 * result + (enablePulseFade?.hashCode() ?: 0)
        result = 31 * result + (pulseColor ?: 0)
        result = 31 * result + (bearingTintColor ?: 0)
        result = 31 * result + (foregroundTintColor ?: 0)
        result = 31 * result + (backgroundTintColor ?: 0)
        result = 31 * result + (foregroundStaleTintColor ?: 0)
        result = 31 * result + (backgroundStaleTintColor ?: 0)
        return result
    }
}
