/*
 * This file is part of ramani-maps.
 *
 * Copyright (c) 2026 Roman Bapst & Jonas Vautherin.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.ramani.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ramani.compose.MapLibre
import org.ramani.compose.style.CircleStyleLayer
import org.ramani.compose.style.Expression
import org.ramani.compose.style.GeoJsonSource
import org.ramani.compose.style.SymbolStyleLayer

@Composable
fun ClustersScreen() {
    val pointCount = Expression.toNumber(Expression.get("point_count"))

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        MapLibre(
            modifier = Modifier.fillMaxSize(),
        ) {
            GeoJsonSource(
                id = "earthquakes",
                uri = "https://www.mapbox.com/mapbox-gl-js/assets/earthquakes.geojson",
                cluster = true,
                clusterMaxZoom = 14,
                clusterRadius = 50,
            )

            CircleStyleLayer(
                id = "unclustered",
                sourceId = "earthquakes",
                filter = Expression.has("mag"),
                radius = Expression.const(10f),
                color = Expression.interpolate(
                    Expression.exponential(1),
                    Expression.get("mag"),
                    Expression.stop(2.0, Expression.rgb(0, 255, 0)),
                    Expression.stop(4.5, Expression.rgb(0, 0, 255)),
                    Expression.stop(7.0, Expression.rgb(255, 0, 0)),
                ),
            )

            CircleStyleLayer(
                id = "cluster",
                sourceId = "earthquakes",
                filter = Expression.gt(pointCount, 1),
                color = Expression.color(0xFF000000.toInt()),
                radius = Expression.const(18f),
            )

            SymbolStyleLayer(
                id = "count",
                sourceId = "earthquakes",
                filter = Expression.gt(pointCount, 1),
                textField = Expression.exprToString(pointCount),
                textSize = Expression.const(12f),
                textColor = Expression.color(0xFFFFFFFF.toInt()),
                textIgnorePlacement = true,
                textAllowOverlap = true,
            )
        }
    }
}
