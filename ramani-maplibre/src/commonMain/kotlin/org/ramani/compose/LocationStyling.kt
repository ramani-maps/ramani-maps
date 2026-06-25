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
class LocationStyling(
    val accuracyAlpha: Float? = null,
    val accuracyColor: Int? = null,
    val enablePulse: Boolean? = null,
    val enablePulseFade: Boolean? = null,
    val pulseColor: Int? = null,
    val bearingTintColor: Int? = null,
    val foregroundTintColor: Int? = null,
    val backgroundTintColor: Int? = null,
    val foregroundStaleTintColor: Int? = null,
    val backgroundStaleTintColor: Int? = null,
) {
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
        accuracyColor: Int? = this.accuracyColor,
        enablePulse: Boolean? = this.enablePulse,
        enablePulseFade: Boolean? = this.enablePulseFade,
        pulseColor: Int? = this.pulseColor,
        bearingTintColor: Int? = this.bearingTintColor,
        foregroundTintColor: Int? = this.foregroundTintColor,
        backgroundTintColor: Int? = this.backgroundTintColor,
        foregroundStaleTintColor: Int? = this.foregroundStaleTintColor,
        backgroundStaleTintColor: Int? = this.backgroundStaleTintColor,
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
        if (other == null || other !is LocationStyling) return false

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
