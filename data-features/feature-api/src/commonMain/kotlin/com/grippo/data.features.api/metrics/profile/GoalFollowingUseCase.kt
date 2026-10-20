package com.grippo.data.features.api.metrics.profile

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.GoalPrimaryGoalEnum
import com.grippo.data.features.api.goal.models.PersonalizationKeyEnum
import com.grippo.data.features.api.metrics.profile.models.GoalAdherence
import com.grippo.data.features.api.metrics.profile.models.GoalFitFinding
import com.grippo.data.features.api.metrics.profile.models.GoalFitRule
import com.grippo.data.features.api.metrics.profile.models.GoalFitSeverity
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.math.floor

public class GoalFollowingUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
) {

    public suspend fun fromTrainingsByPrimary(
        trainings: List<Training>,
        goal: Goal,
        range: DateRange,
        now: LocalDateTime,
    ): GoalAdherence {
        val analysisRange = analysisRange(goal = goal, range = range, now = now)
        val scopedTrainings = trainings
            .filter {
                analysisRange != null && it.createdAt >= analysisRange.from && it.createdAt <= analysisRange.to
            }
        val sessions = scopedTrainings.filter { it.id.isNotBlank() }.distinctBy { it.id }
        val sets = sessions.flatMap { training ->
            training.exercises.filter { it.id.isNotBlank() }.distinctBy { it.id }.flatMap { exercise ->
                exercise.iterations.distinctBy { it.id }.mapNotNull { iteration ->
                    loggedSet(training = training, exercise = exercise, iteration = iteration)
                }
            }
        }
        val totalSetCount = scopedTrainings.sumOf { training -> training.exercises.sumOf { it.iterations.size } }
        val ids = sets.map { it.exampleId }.distinct()
        val examples = if (ids.isEmpty()) {
            emptyMap()
        } else {
            exerciseExampleFeature.observeExerciseExamples(ids).first().associateBy { it.value.id }
        }
        val groupCredits = examples.mapValues { groupCredits(it.value) }
        val missingMetadataCount = sets.count { groupCredits[it.exampleId] == null }
        val days = analysisRange?.let { it.from.date.daysUntil(it.to.date) + 1 } ?: 0
        val shares = repShares(sets)
        val findings = if (analysisRange != null && days >= GoalAdherence.MIN_ANALYSIS_DAYS && sets.isNotEmpty()) {
            evaluate(
                goal = goal,
                sets = sets,
                range = analysisRange,
                days = days,
                groupCredits = groupCredits,
                examples = examples,
                hasCompleteMetadata = missingMetadataCount == 0,
            )
        } else {
            emptyList()
        }

        return GoalAdherence(
            goal = goal,
            strengthShare = shares[0],
            hypertrophyShare = shares[1],
            enduranceShare = shares[2],
            sessionCount = sets.map { it.trainingId }.distinct().size,
            findings = findings,
            analysisRange = analysisRange,
            setCount = sets.size,
            excludedSetCount = totalSetCount - sets.size,
            missingMetadataSetCount = missingMetadataCount,
        )
    }

    private fun analysisRange(goal: Goal, range: DateRange, now: LocalDateTime): DateRange? {
        require(range.isWellFormed()) { "Goal analysis range must end at or after its start." }
        val from = maxOf(range.from, goal.createdAt)
        val to = minOf(range.to, now)
        if (from > to) {
            return null
        }
        return DateRange(from = from, to = to)
    }

    private fun loggedSet(training: Training, exercise: Exercise, iteration: Iteration): LoggedSet? {
        if (iteration.id.isBlank() || iteration.repetitions <= 0 || exercise.exerciseExample.id.isBlank()) {
            return null
        }
        val values = listOf(
            iteration.externalWeight,
            iteration.extraWeight,
            iteration.assistWeight,
            iteration.bodyWeight,
            iteration.bodyMultiplier,
        )
        if (values.any { it != null && (!it.isFinite() || it < 0f) }) {
            return null
        }

        val weightType = exercise.exerciseExample.weightType
        val externalLoad = iteration.externalWeight?.takeIf {
            it > 0f && iteration.extraWeight == null && iteration.assistWeight == null &&
                iteration.bodyWeight == null && iteration.bodyMultiplier == null
        }

        return LoggedSet(
            trainingId = training.id,
            date = training.createdAt.date,
            exampleId = exercise.exerciseExample.id,
            category = exercise.exerciseExample.category,
            weightType = weightType,
            reps = iteration.repetitions,
            externalLoad = externalLoad,
        )
    }

    /** Rep buckets describe logged sets; they do not estimate effort or physiological stimulus. */
    private fun repShares(sets: List<LoggedSet>): List<Int> {
        if (sets.isEmpty()) {
            return listOf(0, 0, 0)
        }
        val counts = listOf(
            sets.count { it.reps <= 5 },
            sets.count { it.reps in 6..15 },
            sets.count { it.reps >= 16 },
        )
        val exact = counts.map { it.toDouble() * 100.0 / sets.size.toDouble() }
        val shares = exact.map { floor(it).toInt() }.toMutableList()
        val remainder = 100 - shares.sum()
        exact.indices.sortedByDescending { exact[it] - shares[it] }.take(remainder).forEach { index ->
            shares[index] += 1
        }
        return shares
    }

    private fun evaluate(
        goal: Goal,
        sets: List<LoggedSet>,
        range: DateRange,
        days: Int,
        groupCredits: Map<String, Map<MuscleGroupEnum, Float>?>,
        examples: Map<String, ExerciseExample>,
        hasCompleteMetadata: Boolean,
    ): List<GoalFitFinding> {
        val findings = mutableListOf<GoalFitFinding>()
        val weeks = days.toFloat() / 7f
        val frequency = sets.map { it.date }.distinct().size.toFloat() / weeks
        val schedule = schedule(goal)
        val weeklySets = completeWeeklySets(sets = sets, range = range)
        val deload = PersonalizationKeyEnum.PHASE_DELOAD in goal.personalizations

        when (goal.primaryGoal) {
            GoalPrimaryGoalEnum.BUILD_MUSCLE -> {
                schedule?.let {
                    findings += frequencyFinding(
                        rule = GoalFitRule.BUILD_MUSCLE_WEEKLY_FREQUENCY,
                        actual = frequency,
                        schedule = it,
                    )
                }
                if (hasCompleteMetadata) {
                    val groups = PRIMARY_GROUPS + focusedGroups(goal)
                    groups.forEach { group ->
                        val credit = sets.sumOf {
                            (groupCredits[it.exampleId]?.get(group) ?: 0f).toDouble()
                        }.toFloat()
                        val target = if (group in PRIMARY_GROUPS) 10f else 6f
                        if (!avoidsVolumeTargets(goal)) {
                            findings += atLeast(
                                rule = GoalFitRule.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
                                actual = credit / weeks,
                                passMin = target,
                                warnMin = target / 2f,
                                context = listOf(group),
                            )
                        }
                        val trainingDays = sets.filter {
                            (groupCredits[it.exampleId]?.get(group) ?: 0f) >= 0.5f
                        }.map { it.date }.distinct().size
                        findings += atLeast(
                            rule = GoalFitRule.BUILD_MUSCLE_WEEKLY_FREQUENCY_PER_PRIMARY_GROUP,
                            actual = trainingDays.toFloat() / weeks,
                            passMin = 1f,
                            warnMin = 0.5f,
                            context = listOf(group),
                        )
                    }
                }
                if (!deload && !avoidsLoadTargets(goal)) {
                    progression(sets = sets, examples = examples)?.let { trend ->
                        findings += progressionFinding(
                            rule = GoalFitRule.BUILD_MUSCLE_LOAD_PROGRESSION,
                            trend = trend,
                        )
                    }
                }
            }

            GoalPrimaryGoalEnum.GET_STRONGER -> {
                schedule?.let {
                    findings += frequencyFinding(
                        rule = GoalFitRule.GET_STRONGER_WEEKLY_FREQUENCY,
                        actual = frequency,
                        schedule = it,
                    )
                }
                if (!deload && !avoidsLoadTargets(goal)) {
                    val lowRepShare = sets.count { it.reps <= 5 }.toFloat() * 100f / sets.size.toFloat()
                    findings += atLeast(
                        rule = GoalFitRule.GET_STRONGER_STRENGTH_REP_RANGE_SHARE,
                        actual = lowRepShare,
                        passMin = 40f,
                        warnMin = 20f,
                    )
                    val compoundShare = sets.count {
                        it.category == CategoryEnum.COMPOUND
                    }.toFloat() * 100f / sets.size.toFloat()
                    findings += atLeast(
                        rule = GoalFitRule.GET_STRONGER_COMPOUND_SET_SHARE,
                        actual = compoundShare,
                        passMin = 60f,
                        warnMin = 40f,
                    )
                    progression(sets = sets, examples = examples)?.let { trend ->
                        findings += progressionFinding(
                            rule = GoalFitRule.GET_STRONGER_STRENGTH_PROGRESSION,
                            trend = trend,
                        )
                    }
                }
            }

            GoalPrimaryGoalEnum.MAINTAIN -> {
                schedule?.let {
                    findings += frequencyFinding(
                        rule = GoalFitRule.MAINTAIN_WEEKLY_FREQUENCY,
                        actual = frequency,
                        schedule = it,
                    )
                }
                if (hasCompleteMetadata) {
                    findings += coverageFinding(
                        rule = GoalFitRule.MAINTAIN_GROUP_COVERAGE,
                        sets = sets,
                        credits = groupCredits,
                        weeks = weeks,
                    )
                }
                if (!deload) {
                    stability(weeklySets)?.let { actual ->
                        findings += atMost(
                            rule = GoalFitRule.MAINTAIN_WEEKLY_VOLUME_STABILITY,
                            actual = actual,
                            passMax = 0.3f,
                            warnMax = 0.6f,
                        )
                    }
                }
            }

            GoalPrimaryGoalEnum.GENERAL_FITNESS -> {
                schedule?.let {
                    findings += frequencyFinding(
                        rule = GoalFitRule.GENERAL_FITNESS_WEEKLY_FREQUENCY,
                        actual = frequency,
                        schedule = it,
                    )
                }
                if (hasCompleteMetadata) {
                    findings += coverageFinding(
                        rule = GoalFitRule.GENERAL_FITNESS_GROUP_COVERAGE,
                        sets = sets,
                        credits = groupCredits,
                        weeks = weeks,
                    )
                }
            }

            GoalPrimaryGoalEnum.LOSE_FAT -> {
                schedule?.let {
                    findings += frequencyFinding(
                        rule = GoalFitRule.LOSE_FAT_SESSION_ADHERENCE,
                        actual = frequency,
                        schedule = it,
                    )
                }
                findings += atLeast(
                    rule = GoalFitRule.LOSE_FAT_RESISTANCE_PRESENCE,
                    actual = sets.size.toFloat() / weeks,
                    passMin = 6f,
                    warnMin = 2f,
                )
                val previousWeek = weeklySets.getOrNull(weeklySets.lastIndex - 1)
                if (!deload && previousWeek != null && previousWeek > 0f) {
                    findings += atLeast(
                        rule = GoalFitRule.LOSE_FAT_WEEKLY_VOLUME_TREND,
                        actual = weeklySets.last() / previousWeek,
                        passMin = 0.85f,
                        warnMin = 0.7f,
                    )
                }
            }

            GoalPrimaryGoalEnum.RETURN_TO_TRAINING -> {
                schedule?.let {
                    findings += frequencyFinding(
                        rule = GoalFitRule.RETURN_TO_TRAINING_WEEKLY_FREQUENCY,
                        actual = frequency,
                        schedule = it,
                    )
                }
                if (!deload) {
                    val maximumIncrease = weeklySets.zipWithNext()
                        .filter { (before, _) -> before > 0f }
                        .maxOfOrNull { (before, after) -> after / before }
                    if (maximumIncrease != null) {
                        findings += atMost(
                            rule = GoalFitRule.RETURN_TO_TRAINING_LOAD_RAMP_GENTLE,
                            actual = maximumIncrease,
                            passMax = 1.5f,
                            warnMax = 2f,
                        )
                    }
                }
            }
        }
        return findings
    }

    /** Product heuristic: normalized group shares earn partial set credit, never measured effort. */
    private fun groupCredits(example: ExerciseExample): Map<MuscleGroupEnum, Float>? {
        if (example.bundles.isEmpty() || example.bundles.any { it.percentage < 0 }) {
            return null
        }
        val total = example.bundles.sumOf { it.percentage.toDouble() }
        if (total <= 0.0) {
            return null
        }
        val shares = mutableMapOf<MuscleGroupEnum, Float>()
        example.bundles.forEach { bundle ->
            val percent = (bundle.percentage.toDouble() * 100.0 / total).toFloat()
            val group = bundle.muscle.type.group()
            shares[group] = (shares[group] ?: 0f) + percent / 100f
        }
        return shares.mapValues { (_, share) ->
            val synergist = 0.5f * smoothStep((share - 0.1f) / 0.05f)
            val agonist = 0.5f * smoothStep((share - 0.3f) / 0.05f)
            synergist + agonist
        }.filterValues { it > 0f }.takeIf { it.isNotEmpty() }
    }

    private fun focusedGroups(goal: Goal): Set<MuscleGroupEnum> {
        val keys = goal.personalizations
        return buildSet {
            if (keys.any { it in ARM_FOCUS }) {
                add(MuscleGroupEnum.ARMS_AND_FOREARMS)
            }
            if (keys.any { it in SHOULDER_FOCUS }) {
                add(MuscleGroupEnum.SHOULDER_MUSCLES)
            }
            if (PersonalizationKeyEnum.FOCUS_ABS_AND_CORE in keys) {
                add(MuscleGroupEnum.ABDOMINAL_MUSCLES)
            }
        }
    }

    private fun schedule(goal: Goal): Schedule? {
        val explicit = goal.personalizations.filter { it in SCHEDULE_KEYS }.distinct()
        if (explicit.size > 1) {
            return null
        }
        return when (explicit.firstOrNull()) {
            PersonalizationKeyEnum.LOW_FREQUENCY -> Schedule(min = 1f, max = 2f)
            PersonalizationKeyEnum.MODERATE_FREQUENCY -> Schedule(min = 3f, max = 4f)
            PersonalizationKeyEnum.HIGH_FREQUENCY -> Schedule(min = 5f, max = null)
            else -> if (goal.primaryGoal == GoalPrimaryGoalEnum.RETURN_TO_TRAINING) {
                Schedule(min = 1f, max = 3f)
            } else {
                Schedule(min = 2f, max = null)
            }
        }
    }

    private fun avoidsLoadTargets(goal: Goal): Boolean {
        return goal.personalizations.any { it in LOAD_TARGET_RESTRICTIONS }
    }

    private fun avoidsVolumeTargets(goal: Goal): Boolean {
        return goal.personalizations.any { it in VOLUME_TARGET_RESTRICTIONS }
    }

    private fun coverageFinding(
        rule: GoalFitRule,
        sets: List<LoggedSet>,
        credits: Map<String, Map<MuscleGroupEnum, Float>?>,
        weeks: Float,
    ): GoalFitFinding {
        val coverage = PRIMARY_GROUPS.count { group ->
            sets.sumOf { (credits[it.exampleId]?.get(group) ?: 0f).toDouble() } / weeks.toDouble() >= 1.0
        }
        return atLeast(rule = rule, actual = coverage.toFloat(), passMin = 3f, warnMin = 2f)
    }

    /** Only completed seven-day blocks are comparable; partial days at either edge are excluded. */
    private fun completeWeeklySets(sets: List<LoggedSet>, range: DateRange): List<Float> {
        val firstDate = if (range.from == DateTimeUtils.startOfDay(range.from)) {
            range.from.date
        } else {
            range.from.date.plus(DatePeriod(days = 1))
        }
        val lastDate = if (range.to >= DateTimeUtils.endOfDay(range.to)) {
            range.to.date
        } else {
            range.to.date.plus(DatePeriod(days = -1))
        }
        val completeWeeks = ((firstDate.daysUntil(lastDate) + 1) / 7).coerceAtLeast(0)
        val counts = IntArray(completeWeeks)
        sets.forEach { set ->
            val offset = firstDate.daysUntil(set.date)
            if (offset >= 0 && offset / 7 < completeWeeks) {
                counts[offset / 7] += 1
            }
        }
        return counts.map { it.toFloat() }
    }

    private fun stability(weeks: List<Float>): Float? {
        if (weeks.size < 2) {
            return null
        }
        val average = weeks.average().toFloat()
        if (average <= 0f) {
            return null
        }
        return (weeks.max() - weeks.min()) / average
    }

    private fun progression(sets: List<LoggedSet>, examples: Map<String, ExerciseExample>): Float? {
        val trends = sets
            .filter {
                it.weightType != WeightTypeEnum.BODY_WEIGHT && it.externalLoad != null && it.reps <= 10 &&
                    examples[it.exampleId]?.components is ExerciseExampleComponents.External
            }
            .groupBy { it.exampleId }
            .values
            .mapNotNull { exerciseSets ->
                val byDate = exerciseSets.groupBy { it.date }.entries.sortedBy { it.key }
                if (byDate.size < 3 || byDate.first().key.daysUntil(byDate.last().key) < 14) {
                    return@mapNotNull null
                }
                val estimates = byDate.mapNotNull { (_, daySets) ->
                    daySets.mapNotNull { set ->
                        val load = set.externalLoad
                        if (load == null) {
                            null
                        } else {
                            val estimate = if (set.reps == 1) {
                                load
                            } else {
                                load * (1f + set.reps.toFloat() / 30f)
                            }
                            estimate.takeIf { it.isFinite() && it > 0f }
                        }
                    }.maxOrNull()
                }
                if (estimates.size != byDate.size) {
                    return@mapNotNull null
                }
                val half = estimates.size / 2
                val before = median(estimates.take(half))
                val after = median(estimates.takeLast(half))
                val trend = (after - before) / before
                trend.takeIf { it.isFinite() }
            }
        return if (trends.isEmpty()) {
            null
        } else {
            median(trends)
        }
    }

    private fun progressionFinding(rule: GoalFitRule, trend: Float): GoalFitFinding {
        return atLeast(rule = rule, actual = trend, passMin = 0f, warnMin = -0.05f)
    }

    private fun frequencyFinding(rule: GoalFitRule, actual: Float, schedule: Schedule): GoalFitFinding {
        val maximum = schedule.max
        val severity = when {
            actual >= schedule.min && (maximum == null || actual <= maximum) -> GoalFitSeverity.PASS
            actual >= schedule.min * 0.5f && (maximum == null || actual <= maximum + 1f) -> GoalFitSeverity.WARN
            else -> GoalFitSeverity.FAIL
        }
        return GoalFitFinding(
            rule = rule,
            severity = severity,
            actualValue = actual,
            targetMin = schedule.min,
            targetMax = maximum,
        )
    }

    private fun atLeast(
        rule: GoalFitRule,
        actual: Float,
        passMin: Float,
        warnMin: Float,
        context: List<MuscleGroupEnum> = emptyList(),
    ): GoalFitFinding {
        val severity = when {
            actual >= passMin -> GoalFitSeverity.PASS
            actual >= warnMin -> GoalFitSeverity.WARN
            else -> GoalFitSeverity.FAIL
        }
        return GoalFitFinding(
            rule = rule,
            severity = severity,
            actualValue = actual,
            targetMin = passMin,
            targetMax = null,
            context = context,
        )
    }

    private fun atMost(rule: GoalFitRule, actual: Float, passMax: Float, warnMax: Float): GoalFitFinding {
        val severity = when {
            actual <= passMax -> GoalFitSeverity.PASS
            actual <= warnMax -> GoalFitSeverity.WARN
            else -> GoalFitSeverity.FAIL
        }
        return GoalFitFinding(
            rule = rule,
            severity = severity,
            actualValue = actual,
            targetMin = null,
            targetMax = passMax,
        )
    }

    private fun smoothStep(value: Float): Float {
        val fraction = value.coerceIn(0f, 1f)
        return fraction * fraction * (3f - 2f * fraction)
    }

    private fun median(values: List<Float>): Float {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            ((sorted[middle - 1].toDouble() + sorted[middle].toDouble()) / 2.0).toFloat()
        }
    }

    private data class LoggedSet(
        val trainingId: String,
        val date: LocalDate,
        val exampleId: String,
        val category: CategoryEnum,
        val weightType: WeightTypeEnum,
        val reps: Int,
        val externalLoad: Float?,
    )

    private data class Schedule(val min: Float, val max: Float?)

    private companion object {
        private val PRIMARY_GROUPS = setOf(
            MuscleGroupEnum.CHEST_MUSCLES,
            MuscleGroupEnum.BACK_MUSCLES,
            MuscleGroupEnum.LEGS,
        )
        private val SCHEDULE_KEYS = setOf(
            PersonalizationKeyEnum.LOW_FREQUENCY,
            PersonalizationKeyEnum.MODERATE_FREQUENCY,
            PersonalizationKeyEnum.HIGH_FREQUENCY,
        )
        private val ARM_FOCUS = setOf(
            PersonalizationKeyEnum.FOCUS_ARMS,
            PersonalizationKeyEnum.FOCUS_FOREARMS_AND_GRIP,
        )
        private val SHOULDER_FOCUS = setOf(
            PersonalizationKeyEnum.FOCUS_SHOULDERS,
            PersonalizationKeyEnum.FOCUS_REAR_DELTS,
        )
        private val LOAD_TARGET_RESTRICTIONS = setOf(
            PersonalizationKeyEnum.AVOID_HEAVY_LOADING,
            PersonalizationKeyEnum.AVOID_HIGH_INTENSITY,
            PersonalizationKeyEnum.NEEDS_SUPPORTED_MOVEMENTS,
            PersonalizationKeyEnum.PREFER_MACHINES,
            PersonalizationKeyEnum.RECOVERING_FROM_INJURY,
            PersonalizationKeyEnum.RECOVERY_FOCUS,
        )
        private val VOLUME_TARGET_RESTRICTIONS = setOf(
            PersonalizationKeyEnum.LOWER_VOLUME,
            PersonalizationKeyEnum.RECOVERING_FROM_INJURY,
            PersonalizationKeyEnum.RECOVERY_FOCUS,
            PersonalizationKeyEnum.FATIGUE_MANAGEMENT,
            PersonalizationKeyEnum.PHASE_DELOAD,
        )
    }
}
