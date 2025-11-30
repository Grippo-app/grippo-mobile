package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.config.SuggestionMath
import com.grippo.data.features.suggestions.prompt.exercise.example.model.MuscleTarget
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.ExercisePromptSection
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.formatOneDecimal

/**
 * Example output:
 * ```
 * Muscle targets to cover:
 *  - Quads: avg 2.0, done 0.0 (needs +2.0)
 * Primary focus: Quads, Glutes
 * ```
 * When several deficits exist (still capped by MAX_MUSCLE_TARGET_LINES):
 * ```
 * Muscle targets to cover:
 *  - Lats: avg 1.8, done 0.5 (needs +1.3)
 *  - Rear Delt: avg 1.2, done 0.0 (needs +1.2)
 *  - Biceps: avg 1.0, done 0.4 (needs +0.6)
 *  - Traps: avg 0.8, done 0.0 (needs +0.8)
 * Primary focus: Lats, Rear Delt, Biceps
 * ```
 * Section is omitted if no positive deficits are found.
 */
internal class MuscleTargetsSection(
    private val targets: List<MuscleTarget>
) : ExercisePromptSection {

    private companion object {
        private const val MAX_PRIMARY_FOCUS = 3
        private const val MAX_MUSCLE_TARGET_LINES = 8
    }

    override val id: String = "muscle_targets"
    override val description: String = "Primary muscle deficits"

    override fun render(into: StringBuilder) {
        val positive = targets.filter { it.deficit > SuggestionMath.DEFICIT_EPS }
            .take(MAX_MUSCLE_TARGET_LINES)
        if (positive.isEmpty()) return

        into.appendLine()
        into.appendLine("Muscle targets to cover:")
        positive.forEach { target ->
            into.appendLine(
                " - ${target.name}: avg ${formatOneDecimal(target.average)}, done ${
                    formatOneDecimal(target.current)
                } (needs +${formatOneDecimal(target.deficit)})"
            )
        }
        val focusNames = positive.take(MAX_PRIMARY_FOCUS).joinToString { it.name }
        into.appendLine("Primary focus: $focusNames")
    }
}
