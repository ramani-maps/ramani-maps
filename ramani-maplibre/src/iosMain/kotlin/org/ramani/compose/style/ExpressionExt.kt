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

import MapLibre.expressionWithMLNJSONObject
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSExpression
import platform.Foundation.NSNumber
import platform.Foundation.NSPredicate

/**
 * Converts a common [Expression] to an [NSExpression] compatible with MapLibre iOS.
 *
 * Uses the JSON expression format via [NSExpression.expressionWithMLNJSONObject].
 * See: https://maplibre.org/maplibre-style-spec/expressions/
 */
@OptIn(ExperimentalForeignApi::class)
internal fun Expression.toNSExpression(): NSExpression {
    val json = toJsonObject()
    return NSExpression.expressionWithMLNJSONObject(json)
}

/**
 * Converts a common [Expression] to a JSON-compatible object matching the
 * MapLibre Style Spec expression format.
 *
 * Returns nested List/Map/String/Number structures suitable for
 * [NSExpression.expressionWithMLNJSONObject].
 */
private fun Expression.toJsonObject(): Any = when (this) {
    is Expression.Const -> value.toJsonValue()
    is Expression.Get -> listOf("get", key)
    is Expression.Has -> listOf("has", key)
    is Expression.Gt -> listOf(">", lhs.toJsonObject(), rhs.toJsonObject())
    is Expression.ToNumber -> listOf("to-number", expr.toJsonObject())
    is Expression.ExprToString -> listOf("to-string", expr.toJsonObject())
    is Expression.Interpolate -> {
        val curveType = when (type) {
            is Expression.InterpolationType.Exponential ->
                listOf("exponential", type.base)
        }
        val result = mutableListOf<Any>("interpolate", curveType, input.toJsonObject())
        for (stop in stops) {
            result.add(stop.input.toJsonValue())
            result.add(stop.output.toJsonObject())
        }
        result
    }
    is Expression.Rgb -> listOf("rgb", r, g, b)
    is Expression.ColorInt -> {
        val a = ((argb shr 24) and 0xFF)
        val r = ((argb shr 16) and 0xFF)
        val g = ((argb shr 8) and 0xFF)
        val b = (argb and 0xFF)
        listOf("rgba", r, g, b, a / 255.0)
    }
    is Expression.SwitchCase -> {
        val result = mutableListOf<Any>("case")
        for ((condition, output) in cases) {
            result.add(condition.toJsonObject())
            result.add(output.toJsonObject())
        }
        result.add(fallback.toJsonObject())
        result
    }
}

private fun Any.toJsonValue(): Any = when (this) {
    is Int, is Long, is Float, is Double, is Boolean, is String -> this
    else -> toString()
}

/**
 * Converts a common [Expression] to an [NSPredicate] for use as a layer filter.
 */
internal fun Expression.toNSPredicate(): NSPredicate = when (this) {
    is Expression.Has -> NSPredicate.predicateWithFormat("${key} != NIL")
    is Expression.Gt -> {
        val lhsKey = (lhs as? Expression.Get)?.key
            ?: (lhs as? Expression.ToNumber)?.let { (it.expr as? Expression.Get)?.key }
        val rhsValue = (rhs as? Expression.Const)?.value
        if (lhsKey != null && rhsValue != null) {
            NSPredicate.predicateWithFormat("$lhsKey > %@", rhsValue)
        } else {
            NSPredicate.predicateWithValue(true)
        }
    }
    else -> {
        NSPredicate.predicateWithValue(true)
    }
}
