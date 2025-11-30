package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.model.MuscleTarget
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.ExercisePromptSection
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.formatOneDecimal

/**
 * Example output:
 * ```
 * Residual fatigue by muscle (0..1, higher = less recovered):
 *  - Quads: 0.3
 * ```
 * With multiple entries (sorted by fatigue, truncated to MAX_MUSCLE_SUMMARY):
 * ```
 * Residual fatigue by muscle (0..1, higher = less recovered):
 *  - Quads: 0.7
 *  - Glutes: 0.5
 *  - Calves: 0.1
 * ```
 * Section is skipped if no residual fatigue data is available.
 */
internal class ResidualFatigueSection(
    private val residuals: Map<String, Double>,
    private val muscleTargets: List<MuscleTarget>
) : ExercisePromptSection {

    private companion object {
        private const val MAX_MUSCLE_SUMMARY = 12
    }

    override val id: String = "residual_fatigue"
    override val description: String = "Residual fatigue snapshot"

    override fun render(into: StringBuilder) {
        if (residuals.isEmpty()) return
        into.appendLine()
        into.appendLine("Residual fatigue by muscle (0..1, higher = less recovered):")
        residuals.entries
            .sortedByDescending { it.value }
            .take(MAX_MUSCLE_SUMMARY)
            .forEach { (id, rf) ->
                val name = muscleTargets.find { it.id == id }?.name ?: id
                into.appendLine(" - $name: ${formatOneDecimal(rf)}")
            }
    }
}
