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

fun MapStyle.toBuilder(): Style.Builder = when (this) {
    is MapStyle.Uri -> Style.Builder().fromUri(uri)
    is MapStyle.Json -> Style.Builder().fromJson(json)
}
