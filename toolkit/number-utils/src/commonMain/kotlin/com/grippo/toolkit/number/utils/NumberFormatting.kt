package com.grippo.toolkit.number.utils

import kotlin.math.abs
import kotlin.math.round

/**
 * Formats a finite number with grouped whole digits and at most one decimal place.
 * Nonzero values below that precision use `<0.1` with the supplied decimal separator.
 */
public fun formatDecimal(value: Double, decimalSeparator: String): String {
    val rounded = if (abs(value) <= Double.MAX_VALUE / 10.0) round(value * 10.0) / 10.0 else value
    if (value > 0.0 && rounded == 0.0) return "<0${decimalSeparator}1"
    if (value < 0.0 && rounded == 0.0) return "−<0${decimalSeparator}1"
    if (rounded == 0.0) return "0"

    val text = rounded.toString().removeSuffix(".0")
    if ('E' in text || 'e' in text) return text.replace(".", decimalSeparator)

    val sign = if (text.startsWith('-')) "-" else ""
    val parts = text.removePrefix("-").split('.')
    val whole = sign + parts.first().reversed().chunked(3).joinToString(" ").reversed()
    val fraction = parts.getOrNull(1) ?: return whole
    return "$whole$decimalSeparator$fraction"
}
