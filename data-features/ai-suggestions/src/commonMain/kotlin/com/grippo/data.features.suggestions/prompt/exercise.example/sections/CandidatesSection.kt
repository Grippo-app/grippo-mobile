package com.grippo.data.features.suggestions.prompt.exercise.example.sections

import com.grippo.data.features.suggestions.prompt.exercise.example.config.HabitCycleConfig
import com.grippo.data.features.suggestions.prompt.exercise.example.config.PromptGuidelineConfig
import com.grippo.data.features.suggestions.prompt.exercise.example.config.SuggestionMath
import com.grippo.data.features.suggestions.prompt.exercise.example.model.ExampleContext
import com.grippo.data.features.suggestions.prompt.exercise.example.model.MuscleTarget
import com.grippo.data.features.suggestions.prompt.exercise.example.model.PredictionSignals
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.ExercisePromptSection
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.formatOneDecimal
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.unrecoveredWeightedShare
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlin.math.roundToInt

/**
 * Example output:
 * ```
 * Candidates:
 *  - leg_press: Leg Press; muscles Quads(60), Glutes(40); tags compound/push/machine/intermediate; usage 3; lastUsed 2024-01-15 (helps +1.0 on Quads) [cycle: due]
 * ```
 * Or when a bodyweight candidate has no equipment/cycle notes:
 * ```
 * Candidates:
 *  - plank_hold: Plank Hold; muscles Abs(70); tags isolation/core/bodyweight/beginner; usage 0; lastUsed never [unrecW 20%]
 * ```
 * For a mixed list with equipment and anti-monotony notes:
 * ```
 * Candidates:
 *  - front_squat: Front Squat; muscles Quads(55), Glutes(25), Core(20); tags compound/push/free/advanced; usage 8; lastUsed 2024-01-28 (helps +0.8 on Quads) [cycle: overdue] [recent<48h] | equip rack,barbell
 *  - ring_row: Ring Row; muscles Lats(50), Rear Delt(30); tags compound/pull/bodyweight/intermediate; usage 1; lastUsed 2023-12-12 (helps +0.4 on Lats) [unrecW 75%]
 *  - reverse_crunch: Reverse Crunch; muscles Abs(60); tags isolation/core/bodyweight/beginner; usage 0; lastUsed never
 * ```
 */
internal class CandidatesSection(
    private val now: LocalDateTime,
    private val signals: PredictionSignals,
    private val candidates: List<ExampleContext>
) : ExercisePromptSection {

    private companion object {
        private const val MAX_MUSCLES_PER_EXERCISE = 4
    }

    override val id: String = "candidates"
    override val description: String = "Candidate exercises with metadata"

    override fun render(into: StringBuilder) {
        into.appendLine()
        into.appendLine("Candidates:")
        candidates.forEach { context ->
            val tags = listOf(
                context.category,
                context.forceType,
                context.weightType,
                context.experience
            ).joinToString("/")

            val targetNote = context.primaryMuscleId()
                ?.let { signals.muscleTargets.find { target -> target.id == it } }
                ?.describeTargetImpact()
                .orEmpty()

            val cycleNote = context.primaryMuscleId()
                ?.let { describeCycle(it) }
                .orEmpty()

            val antiMonotony = context.primaryMuscleId()
                ?.let { pm ->
                    val last = signals.lastLoadByMuscleDateTime[pm]
                    if (last == null) "" else {
                        val hours =
                            (DateTimeUtils.asInstant(now) - DateTimeUtils.asInstant(last)).inWholeHours
                        if (hours in 0..PromptGuidelineConfig.ANTI_MONOTONY_HOURS.toLong()) {
                            " [recent<${PromptGuidelineConfig.ANTI_MONOTONY_HOURS}h]"
                        } else ""
                    }
                }.orEmpty()

            val unrecWeighted = context.unrecoveredWeightedShare(signals.residualFatigueByMuscle)
            val unrecNote =
                if (unrecWeighted > 0.0) " [unrecW ${(unrecWeighted * 100).roundToInt()}%]" else ""

            val equipmentNote = if (context.equipmentIds.isNotEmpty()) " | equip ${
                context.equipmentIds.joinToString(",")
            }" else ""

            into.append(
                " - ${context.id}: ${context.displayName}; muscles ${context.describeMuscles()}; " +
                        "tags $tags; usage ${context.usageCount}; lastUsed ${context.lastUsed ?: "never"}" +
                        targetNote + cycleNote + antiMonotony + unrecNote + equipmentNote
            )
            into.appendLine()
        }
    }

    private fun MuscleTarget.describeTargetImpact(): String {
        return when {
            deficit > SuggestionMath.DEFICIT_EPS -> " (helps +${formatOneDecimal(deficit)} on $name)"
            deficit < -SuggestionMath.DEFICIT_EPS -> " (over +${formatOneDecimal(-deficit)} on $name)"
            else -> ""
        }
    }

    private fun describeCycle(muscleId: String): String {
        val habit = signals.periodicHabits[muscleId]
        return when {
            habit == null -> ""
            habit.nextDue < now.date -> " [cycle: overdue]"
            habit.nextDue == now.date -> " [cycle: due]"
            now.date.daysUntil(habit.nextDue) <= HabitCycleConfig.GRACE_DAYS -> " [cycle: soon]"
            else -> ""
        }
    }

    private fun ExampleContext.describeMuscles(): String {
        if (muscles.isEmpty()) return "-"
        return muscles.take(MAX_MUSCLES_PER_EXERCISE)
            .joinToString { "${it.name}(${it.percentage})" }
    }
}
