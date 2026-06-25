/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.compose.style

import androidx.compose.runtime.Composable

@Composable
expect fun GeoJsonSource(
    id: String,
    uri: String? = null,
    geoJson: String? = null,
    cluster: Boolean = false,
    clusterMaxZoom: Int = 14,
    clusterRadius: Int = 50,
)

@Composable
expect fun RasterSource(
    id: String,
    tileJsonUrl: String,
)

@Composable
expect fun VectorSource(
    id: String,
    tileJsonUrl: String,
)
