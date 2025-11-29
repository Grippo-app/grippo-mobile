package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.ExerciseExampleSuggestionPromptBuilder
import com.grippo.data.features.suggestions.prompt.exercise.example.ExerciseExampleSuggestionPromptBuilder.TrainingSummary
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.utils.ExercisePromptSection
import com.grippo.data.features.suggestions.prompt.exercise.example.sections.utils.formatOneDecimal

/**
 * Example output:
 * ```
 * Recent trainings:
 *  1. 2024-02-01T08:00 (Thursday) [vol 5.0, reps 30, inten 0.2]: Bench Press [...] +1 more
 * ```
 * Or when no trainings fall into the lookback window:
 * ```
 * Recent trainings:
 *  - none recorded in lookback window
 * ```
 * With several trainings (note the muscle summaries and ellipsis when needed):
 * ```
 * Recent trainings:
 *  1. 2024-02-05T18:30 (Monday) [vol 7.5, reps 42, inten 0.3]: Front Squat [Quads(60), Glutes(30)], Walking Lunge [Quads(40), Glutes(40)] +1 more
 *  2. 2024-02-03T07:15 (Saturday) [vol 4.0, reps 28, inten 0.2]: Pull-Up [Lats(55), Biceps(25)], Chest Supported Row [Lats(45)]
 * ```
 */
internal class RecentTrainingsSection(
    private val trainings: List<TrainingSummary>
) : ExercisePromptSection {

    override val id: String = "recent_trainings"
    override val description: String = "Recent training recap"

    override fun render(into: StringBuilder) {
        into.appendLine()
        into.appendLine("Recent trainings:")
        if (trainings.isEmpty()) {
            into.appendLine(" - none recorded in lookback window")
            return
        }

        trainings.forEachIndexed { index, tr ->
            val dow = tr.dayOfWeek.name.lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            into.append(
                " ${index + 1}. ${tr.performedAt} ($dow) " +
                        "[vol ${formatOneDecimal(tr.totalVolume.toDouble())}, reps ${tr.totalRepetitions}, inten ${
                            formatOneDecimal(tr.avgIntensity.toDouble())
                        }]: "
            )
            into.append(
                tr.exercises.take(MAX_TRAINING_EXERCISES)
                    .joinToString { it.displayName + summarizeMuscles(it.muscles) })
            if (tr.exercises.size > MAX_TRAINING_EXERCISES) into.append(" +${tr.exercises.size - MAX_TRAINING_EXERCISES} more")
            into.appendLine()
        }
    }

    private fun summarizeMuscles(muscles: List<ExerciseExampleSuggestionPromptBuilder.MuscleShare>): String {
        if (muscles.isEmpty()) return ""
        val dominant = muscles.take(MAX_MUSCLES_PER_EXERCISE)
            .joinToString { "${it.name}(${it.percentage})" }
        return " [$dominant]"
    }
}

private const val MAX_TRAINING_EXERCISES = 16
private const val MAX_MUSCLES_PER_EXERCISE = 4
