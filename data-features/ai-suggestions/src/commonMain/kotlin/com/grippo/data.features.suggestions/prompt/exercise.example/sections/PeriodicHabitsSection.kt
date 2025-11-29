package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.ExerciseExampleSuggestionPromptBuilder.PeriodicHabit
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.utils.ExercisePromptSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.utils.formatOneDecimal
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil

/**
 * Example output:
 * ```
 * Periodic habits (per muscle):
 *  - Hamstrings: every ~5d (last 2024-01-20, next 2024-01-25, in 2d, conf 0.8)
 * ```
 * Example when multiple habits are overdue/due soon:
 * ```
 * Periodic habits (per muscle):
 *  - Quads: every ~4d (last 2024-01-12, next 2024-01-16, overdue by 3d, conf 0.6)
 *  - Lats: every ~6d (last 2024-01-18, next 2024-01-24, due today, conf 0.7)
 *  - Calves: every ~10d (last 2024-01-10, next 2024-01-20, in 1d, conf 0.9)
 * ```
 * Section is omitted when no periodic habits are found.
 */
internal class PeriodicHabitsSection(
    private val now: LocalDateTime,
    private val habits: Map<String, PeriodicHabit>
) : ExercisePromptSection {

    override val id: String = "periodic_habits"
    override val description: String = "Habit cadence reminders"

    override fun render(into: StringBuilder) {
        if (habits.isEmpty()) return
        val today = now.date
        val lines = habits.values
            .sortedBy { it.nextDue }
            .take(MAX_PERIODIC_LINES)
            .joinToString("\n") { h ->
                val status = when {
                    h.nextDue < today -> "overdue by ${h.nextDue.daysUntil(today)}d"
                    h.nextDue == today -> "due today"
                    else -> "in ${today.daysUntil(h.nextDue)}d"
                }
                " - ${h.muscleName}: every ~${h.medianIntervalDays}d (last ${h.lastDate}, next ${h.nextDue}, $status, conf ${
                    formatOneDecimal(h.confidence)
                })"
            }
        into.appendLine()
        into.appendLine("Periodic habits (per muscle):")
        into.appendLine(lines)
    }
}

private const val MAX_PERIODIC_LINES = 8
