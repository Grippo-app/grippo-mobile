package com.grippo.data.features.suggestions.prompt.exercise.example.utils

import com.grippo.data.features.suggestions.prompt.exercise.example.model.ExampleContext

internal fun ExampleContext.unrecoveredWeightedShare(
    residual: Map<String, Double>
): Double {
    if (muscles.isEmpty()) return 0.0
    var acc = 0.0
    for (m in muscles) {
        val rf = (residual[m.id] ?: 0.0).coerceIn(0.0, 1.0)
        if (rf > 0.0) {
            acc += (m.percentage.coerceAtLeast(0)).toDouble() / 100.0 * rf
        }
    }
    return acc.coerceIn(0.0, 1.0)
}

internal fun ExampleContext.isPrimaryRecoveredByResidual(
    residual: Map<String, Double>
): Boolean {
    val pm = primaryMuscleId() ?: return true
    val rf = (residual[pm] ?: 0.0).coerceIn(0.0, 1.0)
    return rf <= 0.0
}

