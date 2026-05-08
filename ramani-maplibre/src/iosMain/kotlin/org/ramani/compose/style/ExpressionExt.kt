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

import platform.Foundation.NSExpression
import platform.Foundation.NSNumber
import platform.Foundation.NSPredicate
import platform.UIKit.UIColor

/**
 * Converts a common [Expression] to an [NSExpression] compatible with MapLibre iOS.
 *
 * MapLibre iOS uses NSExpression with custom MGL function names for style expressions.
 * See: https://maplibre.org/maplibre-native/ios/api/predicates-and-expressions.html
 */
internal fun Expression.toNSExpression(): NSExpression = when (this) {
    is Expression.Const -> NSExpression.expressionForConstantValue(value.toNSValue())
    is Expression.Get -> NSExpression.expressionForKeyPath(key)
    is Expression.Has -> {
        // 'has' is used as a filter, not as an expression value.
        // When used within an expression context, we represent it as a function.
        NSExpression.expressionForFunction(
            "MGL_FUNCTION",
            arguments = listOf(
                NSExpression.expressionForConstantValue("has"),
                NSExpression.expressionForConstantValue(key),
            )
        )
    }
    is Expression.Gt -> {
        NSExpression.expressionForFunction(
            "MGL_FUNCTION",
            arguments = listOf(
                NSExpression.expressionForConstantValue(">"),
                lhs.toNSExpression(),
                rhs.toNSExpression(),
            )
        )
    }
    is Expression.ToNumber -> {
        NSExpression.expressionForFunction(
            "castObject:toType:",
            arguments = listOf(
                expr.toNSExpression(),
                NSExpression.expressionForConstantValue("NSNumber"),
            )
        )
    }
    is Expression.ExprToString -> {
        NSExpression.expressionForFunction(
            "stringValue",
            arguments = listOf(expr.toNSExpression())
        )
    }
    is Expression.Interpolate -> {
        val typeExpr = when (type) {
            is Expression.InterpolationType.Exponential ->
                NSExpression.expressionForConstantValue(
                    mapOf("exponential" to type.base)
                )
        }
        val stopsDict = stops.associate { stop ->
            stop.input.toNSValue() to stop.output.toNSExpression()
        }
        NSExpression.expressionForFunction(
            "mgl_interpolate:withCurveType:parameters:stops:",
            arguments = listOf(
                input.toNSExpression(),
                typeExpr,
                NSExpression.expressionForConstantValue(null),
                NSExpression.expressionForConstantValue(stopsDict),
            )
        )
    }
    is Expression.Rgb -> {
        NSExpression.expressionForConstantValue(
            UIColor(
                red = r / 255.0,
                green = g / 255.0,
                blue = b / 255.0,
                alpha = 1.0,
            )
        )
    }
    is Expression.ColorInt -> {
        val a = ((argb shr 24) and 0xFF) / 255.0
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        NSExpression.expressionForConstantValue(UIColor(red = r, green = g, blue = b, alpha = a))
    }
    is Expression.SwitchCase -> {
        val args = mutableListOf<NSExpression>()
        for ((condition, output) in cases) {
            args.add(condition.toNSExpression())
            args.add(output.toNSExpression())
        }
        args.add(fallback.toNSExpression())
        NSExpression.expressionForFunction(
            "MGL_IF",
            arguments = args,
        )
    }
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

private fun Any.toNSValue(): Any? = when (this) {
    is Int -> NSNumber(int = this)
    is Long -> NSNumber(long = this)
    is Float -> NSNumber(float = this)
    is Double -> NSNumber(double = this)
    is Boolean -> NSNumber(bool = this)
    is String -> this
    else -> this
}
