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

sealed interface Expression {
    data class Const(val value: Any) : Expression
    data class Get(val key: String) : Expression
    data class Has(val key: String) : Expression
    data class Gt(val lhs: Expression, val rhs: Expression) : Expression
    data class ToNumber(val expr: Expression) : Expression
    data class ExprToString(val expr: Expression) : Expression

    data class Interpolate(
        val type: InterpolationType,
        val input: Expression,
        val stops: List<Stop>,
    ) : Expression

    data class Stop(val input: Any, val output: Expression)

    sealed interface InterpolationType {
        data class Exponential(val base: Number) : InterpolationType
    }

    data class Rgb(val r: Int, val g: Int, val b: Int) : Expression
    data class ColorInt(val argb: Int) : Expression

    data class SwitchCase(
        val cases: List<Pair<Expression, Expression>>,
        val fallback: Expression,
    ) : Expression

    companion object {
        fun const(value: Any): Expression = Const(value)
        fun get(key: String): Expression = Get(key)
        fun has(key: String): Expression = Has(key)
        fun gt(lhs: Expression, rhs: Expression): Expression = Gt(lhs, rhs)
        fun gt(lhs: Expression, rhs: Number): Expression = Gt(lhs, Const(rhs))
        fun toNumber(expr: Expression): Expression = ToNumber(expr)
        fun exprToString(expr: Expression): Expression = ExprToString(expr)

        fun interpolate(
            type: InterpolationType,
            input: Expression,
            vararg stops: Stop,
        ): Expression = Interpolate(type, input, stops.toList())

        fun exponential(base: Number): InterpolationType = InterpolationType.Exponential(base)
        fun stop(input: Any, output: Expression): Stop = Stop(input, output)

        fun rgb(r: Int, g: Int, b: Int): Expression = Rgb(r, g, b)
        fun color(argb: Int): Expression = ColorInt(argb)

        fun switchCase(
            vararg casesAndFallback: Expression,
        ): Expression {
            require(casesAndFallback.size >= 3 && casesAndFallback.size % 2 == 1) {
                "switchCase requires pairs of (condition, output) followed by a fallback"
            }
            val cases = (0 until casesAndFallback.size - 1 step 2).map { i ->
                casesAndFallback[i] to casesAndFallback[i + 1]
            }
            return SwitchCase(cases, casesAndFallback.last())
        }
    }
}
