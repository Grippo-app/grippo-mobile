package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.ExerciseExampleSuggestionPromptBuilder
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.utils.ExercisePromptSection

/**
 * Example output:
 * ```
 * Session so far (2 exercises):
 *  - Bench Press [Chest(60), Triceps(30)] (compound/push/free/intermediate)
 *  - Face Pull [Rear Delt(70)] (isolation/pull/cable/beginner)
 * ```
 * Or when no exercises are recorded yet:
 * ```
 * Session so far (0 exercises):
 *  - none recorded; rely on recent history
 * ```
 * With equipment hints and overflow handling:
 * ```
 * Session so far (5 exercises):
 *  - Romanian Deadlift [Hamstrings(60), Glutes(30)] (compound/pull/free/intermediate) | equip barbell
 *  - Hip Thrust [Glutes(70)] (compound/push/machine/intermediate)
 *  - Copenhagen Plank [Adductors(50)] (isolation/core/bodyweight/beginner)
 *  - ...
 *  - ... and 2 more
 * ```
 */
internal class SessionSection(
    private val session: List<ExerciseExampleSuggestionPromptBuilder.ExerciseSummary>
) : ExercisePromptSection {

    override val id: String = "session"
    override val description: String = "Current session summary"

    override fun render(into: StringBuilder) {
        into.appendLine()
        into.appendLine("Session so far (${session.size} exercises):")
        if (session.isEmpty()) {
            into.appendLine(" - none recorded; rely on recent history")
            return
        }

        session.take(MAX_SESSION_EXERCISES).forEach { exercise ->
            val tags = listOf(
                exercise.category,
                exercise.forceType,
                exercise.weightType,
                exercise.experience
            ).joinToString("/")
            val equipmentNote = if (exercise.equipmentIds.isNotEmpty()) {
                " | equip ${exercise.equipmentIds.joinToString(",")}"
            } else ""
            into.appendLine(" - ${exercise.displayName}${exercise.muscleSummary()} ($tags)$equipmentNote")
        }
        if (session.size > MAX_SESSION_EXERCISES) {
            into.appendLine(" - ... and ${session.size - MAX_SESSION_EXERCISES} more")
        }
    }

    private fun ExerciseExampleSuggestionPromptBuilder.ExerciseSummary.muscleSummary(): String {
        if (muscles.isEmpty()) return ""
        val dominant = muscles.take(MAX_MUSCLES_PER_EXERCISE)
            .joinToString { "${it.name}(${it.percentage})" }
        return " [$dominant]"
    }
}

private const val MAX_SESSION_EXERCISES = 12
private const val MAX_MUSCLES_PER_EXERCISE = 4
