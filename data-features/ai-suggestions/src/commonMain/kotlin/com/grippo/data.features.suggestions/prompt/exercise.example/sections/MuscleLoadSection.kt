package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.model.MuscleLoad
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.ExercisePromptSection

/**
 * Example output:
 * ```
 * Muscle load (last 4 sessions):
 *  - Quads: 8 load units over 3 sessions
 * ```
 * If there is no usable history:
 * ```
 * Muscle load (last 0 sessions):
 *  - insufficient data
 * ```
 * A fuller sample with multiple muscles:
 * ```
 * Muscle load (last 6 sessions):
 *  - Quads: 18 load units over 4 sessions
 *  - Hamstrings: 12 load units over 3 sessions
 *  - Calves: 2 load units over 2 sessions
 * ```
 */
internal class MuscleLoadSection(
    private val loads: List<MuscleLoad>,
    private val trainingCount: Int
) : ExercisePromptSection {

    override val id: String = "muscle_loads"
    override val description: String = "Aggregate muscle load"

    override fun render(into: StringBuilder) {
        into.appendLine()
        into.appendLine("Muscle load (last $trainingCount sessions):")
        if (loads.isEmpty()) {
            into.appendLine(" - insufficient data")
            return
        }
        loads.forEach { load ->
            into.appendLine(" - ${load.name}: ${load.total} load units over ${load.sessions} sessions")
        }
    }
}
