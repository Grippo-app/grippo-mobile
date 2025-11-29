package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.sections.utils.ExercisePromptSection
import kotlinx.datetime.LocalDateTime

/**
 * Example output:
 * ```
 * Goal: select the best next exercise example for the user.
 * Reply ONLY with JSON {"exerciseExampleId":"id","reason":"<=500 chars"}.
 * Locale: en-US (write the reason in this language)
 * Now: 2024-02-01T12:00
 * Today: 2024-02-01 (Thursday)
 * ```
 * Or when locale/time differ:
 * ```
 * Goal: select the best next exercise example for the user.
 * Reply ONLY with JSON {"exerciseExampleId":"id","reason":"<=500 chars"}.
 * Locale: ru-RU (write the reason in this language)
 * Now: 2024-03-18T07:45
 * Today: 2024-03-18 (Monday)
 * ```
 * Or in another locale with a different time of day:
 * ```
 * Goal: select the best next exercise example for the user.
 * Reply ONLY with JSON {"exerciseExampleId":"id","reason":"<=500 chars"}.
 *
 * Locale: es-ES (write the reason in this language)
 * Now: 2024-05-05T06:10
 * Today: 2024-05-05 (Sunday)
 * ```
 */
internal class IntroSection(
    private val now: LocalDateTime,
    private val locale: String
) : ExercisePromptSection {

    override val id: String = "intro"
    override val description: String = "Goal, locale, and temporal context"

    override fun render(into: StringBuilder) {
        into.appendLine("Goal: select the best next exercise example for the user.")
        into.appendLine("""Reply ONLY with JSON {"exerciseExampleId":"id","reason":"<=500 chars"}.""")
        into.appendLine()
        into.appendLine("Locale: $locale (write the reason in this language)")
        into.appendLine("Now: $now")
        into.appendLine(
            "Today: ${now.date} (${
                now.dayOfWeek.name.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            })"
        )
    }
}
