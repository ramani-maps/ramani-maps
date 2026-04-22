/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose

import org.maplibre.android.maps.Style

sealed interface MapStyle {
    data class Uri(val uri: String) : MapStyle
    data class Json(val json: String) : MapStyle

    fun toBuilder(): Style.Builder = when (this) {
        is Uri -> Style.Builder().fromUri(uri)
        is Json -> Style.Builder().fromJson(json)
    }

    companion object {
        val Default: MapStyle = Uri("https://demotiles.maplibre.org/style.json")
    }
}
