package com.grippo.data.features.suggestions.prompt.exercise.example.utils

import kotlin.math.abs
import kotlin.math.roundToInt

internal fun formatOneDecimal(value: Double): String {
    val scaled = (value * 10.0).roundToInt() / 10.0
    val intPart = scaled.toInt()
    return if (abs(scaled - intPart) < 1e-4) intPart.toString() else scaled.toString()
}

internal fun formatTitleLabel(raw: String): String {
    val normalized = raw.replace('-', ' ').replace('_', ' ')
    val parts = normalized.split(' ').filter { it.isNotBlank() }
    if (parts.isEmpty()) return raw
    return parts.joinToString(" ") { part ->
        val lower = part.lowercase()
        lower.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
    }
}

internal fun Map<String, Int>.dominantKey(): String? = entries.maxByOrNull { it.value }?.key
