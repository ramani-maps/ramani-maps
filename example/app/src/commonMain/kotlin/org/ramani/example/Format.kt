package org.ramani.example

import kotlin.math.pow
import kotlin.math.roundToLong

internal fun Double.fmt(decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = (this * factor).roundToLong()
    val intPart = rounded / factor.toLong()
    val fracPart = kotlin.math.abs(rounded % factor.toLong())
    return "$intPart.${fracPart.toString().padStart(decimals, '0')}"
}

internal fun Double?.fmt(decimals: Int): String = this?.fmt(decimals) ?: "null"
