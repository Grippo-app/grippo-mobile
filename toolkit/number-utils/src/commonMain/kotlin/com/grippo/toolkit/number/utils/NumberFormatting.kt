package com.grippo.toolkit.number.utils

import kotlin.math.abs
import kotlin.math.round

/**
 * Formats a finite number with grouped whole digits and at most one decimal place.
 * Nonzero values below that precision use `<0.1` with the supplied decimal separator.
 */
public fun formatDecimal(value: Double, decimalSeparator: String): String {
    require(value.isFinite()) { "Decimal formatting requires a finite value" }

    val rounded = if (abs(value) <= Double.MAX_VALUE / 10.0) round(value * 10.0) / 10.0 else value
    if (value > 0.0 && rounded == 0.0) return "<0${decimalSeparator}1"
    if (value < 0.0 && rounded == 0.0) return "−<0${decimalSeparator}1"
    if (rounded == 0.0) return "0"

    val raw = abs(rounded).toString()
    val exponentIndex = raw.indexOf('E', ignoreCase = true)
    val text = if (exponentIndex >= 0) {
        val mantissa = raw.substring(0, exponentIndex)
        val exponent = raw.substring(exponentIndex + 1).toInt()
        val digits = mantissa.replace(".", "")
        val decimalIndex = mantissa.indexOf('.').let { if (it >= 0) it else mantissa.length } + exponent
        when {
            decimalIndex >= digits.length -> digits + "0".repeat(decimalIndex - digits.length)
            decimalIndex <= 0 -> "0." + "0".repeat(-decimalIndex) + digits
            else -> digits.substring(0, decimalIndex) + "." + digits.substring(decimalIndex)
        }
    } else {
        raw
    }

    val sign = if (rounded < 0.0) "-" else ""
    val parts = text.removeSuffix(".0").split('.')
    val whole = sign + parts.first().reversed().chunked(3).joinToString(" ").reversed()
    val fraction = parts.getOrNull(1) ?: return whole
    return "$whole$decimalSeparator$fraction"
}
