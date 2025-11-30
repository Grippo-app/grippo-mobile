package com.grippo.data.features.suggestions.prompt.exercise.example.utils

import kotlin.math.roundToInt

internal object PromptMath {
    fun clamp01(v: Double): Double = when {
        v < 0.0 -> 0.0
        v > 1.0 -> 1.0
        else -> v
    }

    fun medianInt(values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) ((sorted[mid - 1] + sorted[mid]) / 2.0).roundToInt() else sorted[mid]
    }

    fun iqrInt(values: List<Int>): Int {
        if (values.size < 4) return (values.maxOrNull() ?: 0) - (values.minOrNull() ?: 0)
        val sorted = values.sorted()
        val q1 = percentile(sorted, 25.0)
        val q3 = percentile(sorted, 75.0)
        return (q3 - q1).roundToInt()
    }

    fun percentile(sorted: List<Int>, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val rank = (p / 100.0) * (sorted.size - 1)
        val lo = rank.toInt()
        val hi = (lo + 1).coerceAtMost(sorted.lastIndex)
        val frac = rank - lo
        return sorted[lo] * (1 - frac) + sorted[hi] * frac
    }

    fun quantileDouble(sortedValues: List<Double>, p: Double): Double {
        if (sortedValues.isEmpty()) return 0.0
        val clampedP = p.coerceIn(0.0, 1.0)
        val rank = clampedP * (sortedValues.size - 1)
        val lo = rank.toInt()
        val hi = (lo + 1).coerceAtMost(sortedValues.lastIndex)
        val frac = rank - lo
        return sortedValues[lo] * (1 - frac) + sortedValues[hi] * frac
    }
}
