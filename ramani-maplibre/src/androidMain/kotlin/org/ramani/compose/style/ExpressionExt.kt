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

import org.maplibre.android.style.expressions.Expression as MlExpr

internal fun Expression.toMapLibre(): MlExpr = when (this) {
    is Expression.Const -> MlExpr.literal(value)
    is Expression.Get -> MlExpr.get(key)
    is Expression.Has -> MlExpr.has(key)
    is Expression.Gt -> MlExpr.gt(lhs.toMapLibre(), rhs.toMapLibre())
    is Expression.ToNumber -> MlExpr.toNumber(expr.toMapLibre())
    is Expression.ExprToString -> MlExpr.toString(expr.toMapLibre())
    is Expression.Interpolate -> {
        val typeExpr = when (type) {
            is Expression.InterpolationType.Exponential -> MlExpr.exponential(type.base)
        }
        val stopArgs = stops.flatMap { stop ->
            listOf(MlExpr.stop(stop.input, stop.output.toMapLibre()))
        }.toTypedArray()
        MlExpr.interpolate(typeExpr, input.toMapLibre(), *stopArgs)
    }
    is Expression.Rgb -> MlExpr.rgb(r.toFloat(), g.toFloat(), b.toFloat())
    is Expression.ColorInt -> MlExpr.color(argb)
    is Expression.SwitchCase -> {
        val args = cases.flatMap { (condition, output) ->
            listOf(condition.toMapLibre(), output.toMapLibre())
        } + fallback.toMapLibre()
        MlExpr.switchCase(*args.toTypedArray())
    }
}
