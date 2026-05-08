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
expect fun CircleStyleLayer(
    id: String,
    sourceId: String,
    filter: Expression? = null,
    radius: Expression? = null,
    color: Expression? = null,
    opacity: Expression? = null,
)

@Composable
expect fun FillStyleLayer(
    id: String,
    sourceId: String,
    filter: Expression? = null,
    color: Expression? = null,
    opacity: Expression? = null,
)

@Composable
expect fun LineStyleLayer(
    id: String,
    sourceId: String,
    sourceLayer: String? = null,
    filter: Expression? = null,
    color: Expression? = null,
    width: Expression? = null,
    opacity: Expression? = null,
)

@Composable
expect fun SymbolStyleLayer(
    id: String,
    sourceId: String,
    filter: Expression? = null,
    textField: Expression? = null,
    textSize: Expression? = null,
    textColor: Expression? = null,
    textIgnorePlacement: Boolean = false,
    textAllowOverlap: Boolean = false,
)

@Composable
expect fun RasterStyleLayer(
    id: String,
    sourceId: String,
)
