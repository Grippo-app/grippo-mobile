package com.grippo.data.features.api.metrics.profile

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.GoalPrimaryGoalEnum
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase.Companion.AGONIST_GROUP_THRESHOLD
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase.Companion.MINUTES_PER_SET_ESTIMATE
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase.Companion.MIN_RECORDED_MINUTES
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase.Companion.SYNERGIST_GROUP_THRESHOLD
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase.Companion.WORKING_MIN_SETS
import com.grippo.data.features.api.metrics.profile.GoalFollowingUseCase.Companion.WORKING_MIN_WEIGHT_FRACTION
import com.grippo.data.features.api.metrics.profile.models.CompoundLiftKind
import com.grippo.data.features.api.metrics.profile.models.GoalAdherence
import com.grippo.data.features.api.metrics.profile.models.GoalFitFinding
import com.grippo.data.features.api.metrics.profile.models.GoalFitRule
import com.grippo.data.features.api.metrics.profile.models.GoalFitSeverity
import com.grippo.data.features.api.metrics.profile.models.TopExerciseContribution
import com.grippo.data.features.api.metrics.profile.models.TopMuscleContribution
import com.grippo.data.features.api.metrics.profile.models.TopMuscleGroupContribution
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

public class GoalFollowingUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val goalFeature: GoalFeature,
) {

    public suspend fun fromTrainingsByPrimary(trainings: List<Training>): GoalAdherence? {
        val goal = goalFeature.observeGoal().firstOrNull() ?: return null
        val analysis = analyze(trainings, goal.primaryGoal)
        val score = scoreForPrimary(goal.primaryGoal, analysis)
        return analysis.toAdherence(goal = goal, score = score)
    }

    // -------------------------------------------------------------------------
    // Orchestration
    // -------------------------------------------------------------------------

    private suspend fun analyze(
        trainings: List<Training>,
        primaryGoal: GoalPrimaryGoalEnum,
    ): Analysis {
        if (trainings.isEmpty()) return Analysis.EMPTY

        val ids = trainings.flatMap { it.exercises }.map { it.exerciseExample.id }.toSet()
        val examples = loadExamples(ids)

        val perSession: List<SessionStats> = trainings.map { training ->
            SessionStats(training, buildExerciseStats(training.exercises))
        }
        val sessionCount = perSession.count { it.stats.isNotEmpty() }

        val raws = perSession.mapNotNull { (training, stats) ->
            buildRawFromStats(training, stats, examples)
        }
        if (raws.isEmpty()) return Analysis.EMPTY.copy(sessionCount = sessionCount)

        val pool = aggregateRaw(raws)
        val strengthShare = (pool.strengthShare * 100f).roundToInt().coerceIn(0, 100)
        val hypertrophyShare = (pool.hypertrophyShare * 100f).roundToInt().coerceIn(0, 100)
        val enduranceShare = (pool.enduranceShare * 100f).roundToInt().coerceIn(0, 100)

        val allStats = perSession.flatMap { it.stats }
        val totalStimulus = allStats.sumOf { it.stimulus.toDouble() }.toFloat()

        val compoundRatio =
            (weightedCategoryRatioByStimulus(allStats, CategoryEnum.COMPOUND) * 100f)
                .roundToInt()
                .coerceIn(0, 100)

        val muscleTotals = computeMuscleStimulusTotals(allStats, examples)
        val totalMuscle = muscleTotals.values.sumOf { it.toDouble() }.toFloat()
        val topMuscles = buildTopMuscles(muscleTotals, totalMuscle)
        val topMuscleGroups = buildTopMuscleGroups(muscleTotals, totalMuscle)
        val topExercises = buildTopExercises(allStats, totalStimulus)

        val findings = computeGoalFitFindings(
            primary = primaryGoal,
            perSession = perSession,
            examples = examples,
            topMuscleGroups = topMuscleGroups,
            topExerciseExampleIds = topExercises.take(LOAD_PROGRESSION_TOP_N).map { it.exampleId },
            strengthShare = strengthShare,
            hypertrophyShare = hypertrophyShare,
            enduranceShare = enduranceShare,
        )

        return Analysis(
            raw = pool,
            sessionCount = sessionCount,
            strengthShare = strengthShare,
            hypertrophyShare = hypertrophyShare,
            enduranceShare = enduranceShare,
            compoundRatio = compoundRatio,
            topExercises = topExercises,
            topMuscles = topMuscles,
            topMuscleGroups = topMuscleGroups,
            findings = findings,
        )
    }

    private fun Analysis.toAdherence(goal: Goal, score: Int): GoalAdherence = GoalAdherence(
        goal = goal,
        score = score,
        strengthShare = strengthShare,
        hypertrophyShare = hypertrophyShare,
        enduranceShare = enduranceShare,
        sessionCount = sessionCount,
        compoundRatio = compoundRatio,
        topExercises = topExercises,
        topMuscles = topMuscles,
        topMuscleGroups = topMuscleGroups,
        findings = findings,
    )

    // -------------------------------------------------------------------------
    // Per-goal scoring
    // -------------------------------------------------------------------------

    private fun scoreForPrimary(primary: GoalPrimaryGoalEnum, analysis: Analysis): Int =
        when (primary) {
            GoalPrimaryGoalEnum.GET_STRONGER -> scoreStrengthFocus(analysis)
            GoalPrimaryGoalEnum.BUILD_MUSCLE -> scoreHypertrophyFocus(analysis)
            GoalPrimaryGoalEnum.LOSE_FAT -> scoreEnduranceFocus(analysis)
            GoalPrimaryGoalEnum.MAINTAIN -> scorePowerbuilding(analysis)
            GoalPrimaryGoalEnum.GENERAL_FITNESS -> scoreConcurrent(analysis)
            GoalPrimaryGoalEnum.RETURN_TO_TRAINING -> scoreReturnToTraining(analysis)
        }

    private fun scoreStrengthFocus(a: Analysis): Int {
        if (a.isSilent) return 0
        val core = a.raw.strengthShare
        val quality = 0.5f * a.raw.compoundRatio + 0.5f * a.raw.freeWeightRatio
        return toScore(core * (SPEC_CORE_WEIGHT + SPEC_QUALITY_WEIGHT * quality))
    }

    private fun scoreHypertrophyFocus(a: Analysis): Int {
        if (a.isSilent) return 0
        val core = a.raw.hypertrophyShare
        val quality = (a.raw.specializationTop2Percent / SPECIALIZATION_SATURATION_PERCENT)
            .coerceIn(0f, 1f)
        return toScore(core * (SPEC_CORE_WEIGHT + SPEC_QUALITY_WEIGHT * quality))
    }

    private fun scoreEnduranceFocus(a: Analysis): Int {
        if (a.isSilent) return 0
        val core = a.raw.enduranceShare
        val quality = (a.raw.activity / ACTIVITY_QUALITY_SATURATION).coerceIn(0f, 1f)
        return toScore(core * (SPEC_CORE_WEIGHT + SPEC_QUALITY_WEIGHT * quality))
    }

    private fun scorePowerbuilding(a: Analysis): Int {
        if (a.isSilent) return 0
        val balance = 2f * min(a.raw.strengthShare, a.raw.hypertrophyShare)
        val penalty = suppressedAxisPenalty(a.raw.enduranceShare)
        return toScore(balance * penalty)
    }

    private fun scoreConcurrent(a: Analysis): Int {
        if (a.isSilent) return 0
        val balance = 2f * min(a.raw.strengthShare, a.raw.enduranceShare)
        val penalty = suppressedAxisPenalty(a.raw.hypertrophyShare)
        return toScore(balance * penalty)
    }

    private fun scoreReturnToTraining(a: Analysis): Int {
        val work = a.raw.totalWork
        val activityFloor = (work / RTT_WORK_FLOOR).coerceIn(0f, 1f)
        val overdoRange = RTT_WORK_CEILING - RTT_WORK_PEAK
        val overdoPenalty = if (overdoRange <= EPS) 1f
        else (1f - (work - RTT_WORK_PEAK) / overdoRange).coerceIn(0f, 1f)
        return toScore(activityFloor * overdoPenalty)
    }

    private fun suppressedAxisPenalty(suppressedShare: Float): Float {
        val t = (suppressedShare / SUPPRESSED_AXIS_PENALTY_FULL).coerceIn(0f, 1f)
        return 1f - t
    }

    private fun toScore(x: Float): Int {
        val v = if (x.isFinite()) x else 0f
        return (v * 100f).roundToInt().coerceIn(0, 100)
    }

    // -------------------------------------------------------------------------
    // Per-session Raw + pooled aggregation
    // -------------------------------------------------------------------------

    private suspend fun loadExamples(ids: Set<String>): Map<String, ExerciseExample> {
        if (ids.isEmpty()) return emptyMap()
        return exerciseExampleFeature.observeExerciseExamples(ids.toList())
            .first()
            .associateBy { it.value.id }
    }

    private fun buildRawFromStats(
        training: Training,
        stats: List<ExerciseStats>,
        exampleMap: Map<String, ExerciseExample>,
    ): Raw? {
        if (stats.isEmpty()) return null

        val totalSets = stats.sumOf { it.sets.size }
        val minutes = resolveMinutes(training.duration.inWholeSeconds.toFloat() / 60f, totalSets)
            ?: return null

        val dimWork = computeDimensionWork(stats)
        val totalDimWork = dimWork.strength + dimWork.hypertrophy + dimWork.endurance

        val (strengthShare, hypertrophyShare, enduranceShare) = if (totalDimWork > EPS) {
            Triple(
                (dimWork.strength / totalDimWork).coerceIn(0f, 1f),
                (dimWork.hypertrophy / totalDimWork).coerceIn(0f, 1f),
                (dimWork.endurance / totalDimWork).coerceIn(0f, 1f),
            )
        } else {
            Triple(0f, 0f, 0f)
        }

        val stimulus = safe(stats.sumOf { it.stimulus.toDouble() }.toFloat())
        val stimulusPerMin = if (stimulus > EPS) stimulus / minutes else 0f

        val reps = training.repetitions.coerceAtLeast(0)
        val repsPerMin = if (reps > 0) reps.toFloat() / minutes else 0f

        val compoundRatio = weightedCategoryRatioByStimulus(stats, CategoryEnum.COMPOUND)
        val freeWeightRatio = weightedWeightTypeRatioByStimulus(stats, WeightTypeEnum.FREE)
        val specializationTop2Percent = computeSpecializationTop2Percent(stats, exampleMap)

        val activity = safe(
            log1pSafe(stimulusPerMin) +
                    log1pSafe(repsPerMin) +
                    log1pSafe(totalDimWork / minutes)
        )

        return Raw(
            strengthShare = strengthShare,
            hypertrophyShare = hypertrophyShare,
            enduranceShare = enduranceShare,
            compoundRatio = clamp01(compoundRatio),
            freeWeightRatio = clamp01(freeWeightRatio),
            specializationTop2Percent = specializationTop2Percent.coerceIn(0f, 100f),
            activity = activity,
            totalWork = safe(totalDimWork),
        )
    }

    /**
     * Returns the session length in minutes. Uses the recorded duration when it
     * is at least [MIN_RECORDED_MINUTES]; otherwise estimates from set count
     * (≈[MINUTES_PER_SET_ESTIMATE] min/set, clamped). Returns null when the
     * session has no sets.
     */
    private fun resolveMinutes(recordedMinutes: Float, totalSets: Int): Float? {
        if (recordedMinutes.isFinite() && recordedMinutes >= MIN_RECORDED_MINUTES) return recordedMinutes
        if (totalSets <= 0) return null
        return (totalSets.toFloat() * MINUTES_PER_SET_ESTIMATE).coerceIn(
            MIN_ESTIMATED_MINUTES,
            MAX_ESTIMATED_MINUTES
        )
    }

    private fun buildExerciseStats(exercises: List<Exercise>): List<ExerciseStats> {
        return exercises.mapNotNull { exercise ->
            val exampleId = exercise.exerciseExample.id
            if (exampleId.isBlank()) return@mapNotNull null

            val liftSets = exercise.iterations.mapNotNull { it.toLiftSetOrNull() }
            if (liftSets.isEmpty()) return@mapNotNull null

            val oneRm = estimateExerciseOneRm(liftSets)
            val setStats = liftSets.mapNotNull { set ->
                val pct = if (oneRm > EPS) (set.weight / oneRm).coerceIn(0f, 1.2f) else Float.NaN
                val base = setStimulus(set.weight, set.reps, pct)
                if (!base.isFinite() || base <= EPS) return@mapNotNull null
                SetStats(weight = set.weight, reps = set.reps, pct = pct, stimulus = base)
            }
            if (setStats.isEmpty()) return@mapNotNull null

            val stimulus = setStats.sumOf { it.stimulus.toDouble() }.toFloat()
            if (!stimulus.isFinite() || stimulus <= EPS) return@mapNotNull null

            ExerciseStats(
                exercise = exercise,
                exampleId = exampleId,
                sets = setStats,
                stimulus = stimulus,
            )
        }
    }

    private fun aggregateRaw(raws: List<Raw>): Raw {
        if (raws.isEmpty()) return Raw.ZERO

        var sumW = 0f
        var strength = 0f
        var hypertrophy = 0f
        var endurance = 0f
        var compound = 0f
        var free = 0f
        var spec = 0f
        var activity = 0f
        var totalWork = 0f

        raws.forEach { r ->
            val w = sessionWeight(r) ?: return@forEach
            sumW += w
            strength += w * r.strengthShare
            hypertrophy += w * r.hypertrophyShare
            endurance += w * r.enduranceShare
            compound += w * r.compoundRatio
            free += w * r.freeWeightRatio
            spec += w * r.specializationTop2Percent
            activity += w * r.activity
            totalWork += w * r.totalWork
        }

        if (sumW <= EPS) return Raw.ZERO

        return Raw(
            strengthShare = clamp01(strength / sumW),
            hypertrophyShare = clamp01(hypertrophy / sumW),
            enduranceShare = clamp01(endurance / sumW),
            compoundRatio = clamp01(compound / sumW),
            freeWeightRatio = clamp01(free / sumW),
            specializationTop2Percent = (spec / sumW).coerceIn(0f, 100f),
            activity = safe(activity / sumW),
            totalWork = safe(totalWork / sumW),
        )
    }

    /** Pool weight a session contributes — null for degenerate sessions (no work). */
    private fun sessionWeight(raw: Raw): Float? {
        val w = SESSION_WEIGHT_FLOOR + raw.activity
        return if (w.isFinite() && w > EPS) w else null
    }

    private fun computeDimensionWork(stats: List<ExerciseStats>): DimensionWork {
        var strength = 0f
        var hypertrophy = 0f
        var endurance = 0f

        stats.forEach { ex ->
            ex.sets.forEach { set ->
                val base = set.stimulus
                if (!base.isFinite() || base <= EPS) return@forEach

                val ms = strengthMembership(set.reps, set.pct)
                val mh = hypertrophyMembership(set.reps, set.pct)
                val me = enduranceMembership(set.reps, set.pct)

                val mSum = ms + mh + me
                if (!mSum.isFinite() || mSum <= EPS) return@forEach

                val inv = 1f / mSum
                strength += base * (ms * inv)
                hypertrophy += base * (mh * inv)
                endurance += base * (me * inv)
            }
        }

        return DimensionWork(
            strength = safe(strength),
            hypertrophy = safe(hypertrophy),
            endurance = safe(endurance),
        )
    }

    private fun strengthMembership(reps: Int, pct: Float): Float {
        val r = reps.coerceAtLeast(0)
        val repFactor = when {
            r <= 3 -> 1.00f
            r <= 5 -> 0.85f
            r <= 8 -> 0.45f
            r <= 10 -> 0.20f
            else -> 0.05f
        }
        val pctFactor = if (!pct.isFinite()) {
            if (r <= 5) 1.0f else 0.6f
        } else {
            smoothStep((pct - 0.75f) / 0.17f)
        }
        return (repFactor * pctFactor).coerceIn(0f, 1f)
    }

    private fun hypertrophyMembership(reps: Int, pct: Float): Float {
        val r = reps.coerceAtLeast(0)
        val repFactor = when {
            r <= 4 -> 0.10f
            r == 5 -> 0.35f
            r in 6..8 -> 0.75f
            r in 9..12 -> 1.00f
            r in 13..15 -> 0.60f
            else -> 0.30f
        }
        val pctFactor = if (!pct.isFinite()) {
            if (r in 6..12) 1.0f else 0.7f
        } else when {
            pct < 0.50f -> 0.20f
            pct < 0.60f -> 0.70f
            pct < 0.76f -> 1.00f
            pct < 0.86f -> 0.65f
            else -> 0.25f
        }
        return (repFactor * pctFactor).coerceIn(0f, 1f)
    }

    private fun enduranceMembership(reps: Int, pct: Float): Float {
        val r = reps.coerceAtLeast(0)
        val repFactor = when {
            r <= 8 -> 0.05f
            r in 9..12 -> 0.25f
            r in 13..15 -> 0.75f
            else -> 1.00f
        }
        val pctFactor = if (!pct.isFinite()) {
            if (r >= 12) 1.0f else 0.6f
        } else when {
            pct <= 0.55f -> 1.00f
            pct <= 0.65f -> 0.75f
            pct <= 0.75f -> 0.45f
            else -> 0.20f
        }
        return (repFactor * pctFactor).coerceIn(0f, 1f)
    }

    private fun Iteration.toLiftSetOrNull(): LiftSet? {
        val w = volume
        val r = repetitions
        if (!w.isFinite() || w <= EPS || r <= 0) return null
        return LiftSet(weight = w, reps = r)
    }

    private fun computeSpecializationTop2Percent(
        stats: List<ExerciseStats>,
        exampleMap: Map<String, ExerciseExample>,
    ): Float {
        val totals = computeMuscleStimulusTotals(stats, exampleMap)
        val values = totals.values.filter { it.isFinite() && it > EPS }.sortedDescending()
        if (values.isEmpty()) return 0f

        val sum = values.sum()
        if (!sum.isFinite() || sum <= EPS) return 0f

        val top2 = values.take(2).sum()
        val percent = (top2 / sum) * 100f
        return if (percent.isFinite()) percent.coerceIn(0f, 100f) else 0f
    }

    private fun computeMuscleStimulusTotals(
        stats: List<ExerciseStats>,
        exampleMap: Map<String, ExerciseExample>,
    ): Map<MuscleEnum, Float> {
        val totals = mutableMapOf<MuscleEnum, Float>()

        stats.forEach { ex ->
            val bundles = exampleMap[ex.exampleId]?.bundles ?: return@forEach
            if (bundles.isEmpty()) return@forEach

            val base = ex.stimulus
            if (!base.isFinite() || base <= EPS) return@forEach

            val totalShare = bundles.sumOf { it.percentage.coerceAtLeast(0) }
            if (totalShare <= 0) return@forEach
            val denom = totalShare.toFloat()

            bundles.forEach { bundle ->
                val share = bundle.percentage.coerceAtLeast(0).toFloat()
                if (share <= 0f) return@forEach

                val ratio = share / denom
                if (!ratio.isFinite() || ratio <= 0f) return@forEach

                val add = base * ratio
                if (!add.isFinite() || add <= EPS) return@forEach
                totals[bundle.muscle.type] = (totals[bundle.muscle.type] ?: 0f) + add
            }
        }
        return totals
    }

    private fun weightedCategoryRatioByStimulus(
        stats: List<ExerciseStats>,
        category: CategoryEnum,
    ): Float {
        var sum = 0f
        var matched = 0f
        stats.forEach { ex ->
            val w = ex.stimulus
            if (!w.isFinite() || w <= EPS) return@forEach
            sum += w
            if (ex.exercise.exerciseExample.category == category) matched += w
        }
        if (!sum.isFinite() || sum <= EPS) return 0f
        val r = matched / sum
        return if (r.isFinite()) r.coerceIn(0f, 1f) else 0f
    }

    private fun weightedWeightTypeRatioByStimulus(
        stats: List<ExerciseStats>,
        type: WeightTypeEnum,
    ): Float {
        var sum = 0f
        var matched = 0f
        stats.forEach { ex ->
            val w = ex.stimulus
            if (!w.isFinite() || w <= EPS) return@forEach
            sum += w
            if (ex.exercise.exerciseExample.weightType == type) matched += w
        }
        if (!sum.isFinite() || sum <= EPS) return 0f
        val r = matched / sum
        return if (r.isFinite()) r.coerceIn(0f, 1f) else 0f
    }

    // -------------------------------------------------------------------------
    // Top contributors
    // -------------------------------------------------------------------------

    private fun buildTopMuscles(
        muscleTotals: Map<MuscleEnum, Float>,
        totalMuscle: Float,
    ): List<TopMuscleContribution> = muscleTotals.entries
        .filter { it.value.isFinite() && it.value > EPS }
        .sortedByDescending { it.value }
        .take(TOP_MUSCLES_LIMIT)
        .map { (muscle, v) ->
            TopMuscleContribution(
                muscle = muscle,
                share = if (totalMuscle > EPS) ((v / totalMuscle) * 100f).roundToInt()
                    .coerceIn(0, 100) else 0,
            )
        }

    private fun buildTopMuscleGroups(
        muscleTotals: Map<MuscleEnum, Float>,
        totalMuscle: Float,
    ): List<TopMuscleGroupContribution> {
        val groupTotals: Map<MuscleGroupEnum, Float> = muscleTotals.entries
            .filter { it.value.isFinite() && it.value > EPS }
            .groupingBy { it.key.group() }
            .fold(0f) { acc, entry -> acc + entry.value }
        return groupTotals.entries
            .sortedByDescending { it.value }
            .map { (group, v) ->
                TopMuscleGroupContribution(
                    group = group,
                    share = if (totalMuscle > EPS) ((v / totalMuscle) * 100f).roundToInt()
                        .coerceIn(0, 100) else 0,
                )
            }
    }

    private fun buildTopExercises(
        allStats: List<ExerciseStats>,
        totalStimulus: Float,
    ): List<TopExerciseContribution> {
        if (allStats.isEmpty()) return emptyList()
        return allStats.groupBy { it.exampleId }.map { (exampleId, list) ->
            val stimulusSum = list.sumOf { it.stimulus.toDouble() }.toFloat()
            val setsCount = list.sumOf { it.sets.size }
            val heaviest = list.flatMap { it.sets }.maxOfOrNull { it.weight } ?: 0f
            val combinedLiftSets = list.flatMap { stats ->
                stats.sets.map { LiftSet(weight = it.weight, reps = it.reps) }
            }
            val e1rm = estimateExerciseOneRm(combinedLiftSets)
            val first = list.first()
            TopExerciseContribution(
                exampleId = exampleId,
                name = first.exercise.name,
                totalSets = setsCount,
                stimulusShare = if (totalStimulus > EPS) {
                    ((stimulusSum / totalStimulus) * 100f).roundToInt().coerceIn(0, 100)
                } else 0,
                heaviestWeight = if (heaviest.isFinite() && heaviest > 0f) heaviest else 0f,
                estimatedOneRepMax = if (e1rm.isFinite() && e1rm > 0f) e1rm else 0f,
                category = first.exercise.exerciseExample.category,
            )
        }.sortedByDescending { it.stimulusShare }.take(TOP_EXERCISES_LIMIT)
    }

    // -------------------------------------------------------------------------
    // Goal-fit diagnostic — input rollups
    //
    // Walks the same per-session pool the score uses, computes the per-week /
    // per-group / per-exercise rollups that the rule evaluators consume.
    // -------------------------------------------------------------------------

    private fun computeGoalFitFindings(
        primary: GoalPrimaryGoalEnum,
        perSession: List<SessionStats>,
        examples: Map<String, ExerciseExample>,
        topMuscleGroups: List<TopMuscleGroupContribution>,
        topExerciseExampleIds: List<String>,
        strengthShare: Int,
        hypertrophyShare: Int,
        enduranceShare: Int,
    ): List<GoalFitFinding> {
        val context = buildGoalFitContext(
            perSession = perSession,
            examples = examples,
            topMuscleGroups = topMuscleGroups,
            topExerciseExampleIds = topExerciseExampleIds,
            strengthShare = strengthShare,
            hypertrophyShare = hypertrophyShare,
            enduranceShare = enduranceShare,
        )
        if (context.workingSetCount <= 0) return emptyList()
        return when (primary) {
            GoalPrimaryGoalEnum.BUILD_MUSCLE -> evaluateBuildMuscle(context)
            GoalPrimaryGoalEnum.GET_STRONGER -> evaluateGetStronger(context)
            GoalPrimaryGoalEnum.MAINTAIN -> evaluateMaintain(context)
            GoalPrimaryGoalEnum.GENERAL_FITNESS -> evaluateGeneralFitness(context)
            GoalPrimaryGoalEnum.LOSE_FAT -> evaluateLoseFat(context)
            GoalPrimaryGoalEnum.RETURN_TO_TRAINING -> evaluateReturnToTraining(context)
        }
    }

    private fun buildGoalFitContext(
        perSession: List<SessionStats>,
        examples: Map<String, ExerciseExample>,
        topMuscleGroups: List<TopMuscleGroupContribution>,
        topExerciseExampleIds: List<String>,
        strengthShare: Int,
        hypertrophyShare: Int,
        enduranceShare: Int,
    ): GoalFitContext {
        val weeksInPeriod = computeWeeksInPeriod(perSession.map { it.training })
        val sessionCount = perSession.count { it.stats.isNotEmpty() }
        val sessionsPerWeek = sessionCount.toFloat() / weeksInPeriod.toFloat()

        val workingSets = extractWorkingSets(perSession)
        val setsPerWeekPerGroup = computeSetsPerWeekPerGroup(workingSets, examples, weeksInPeriod)
        val sessionsPerWeekPerGroup =
            computeSessionsPerWeekPerGroup(workingSets, examples, weeksInPeriod)
        val repRangeBuckets = workingSets.groupingBy { it.repBucket() }.eachCount()
        val topSets = pickTopSets(workingSets)

        val e1RMByExercise: Map<String, Float> = workingSets
            .groupBy { it.exampleId }
            .mapValues { (_, sets) ->
                estimateExerciseOneRm(sets.map {
                    LiftSet(
                        it.weight,
                        it.reps
                    )
                })
            }
            .filterValues { it > EPS }

        val workingSetsWithStimulus = populateStimulus(workingSets, e1RMByExercise)

        val topSetIntensityShares = computeTopSetIntensityShares(topSets, e1RMByExercise)
        val topSetHeavyShareAt85 =
            computeIntensityShareAt(topSets, e1RMByExercise, RTT_INTENSITY_FLOOR_FRACTION)

        val e1RMTrendByExercise = computeE1RMTrendByExercise(workingSetsWithStimulus)
        val weeklyVolumeSeries = computeWeeklyVolumeSeries(workingSetsWithStimulus)

        val compoundLiftPresence = computeCompoundLiftPresence(workingSets, examples)

        val resistanceSets = workingSets.count { it.externalLoad >= BODYWEIGHT_FLOOR_KG }
        val resistanceSetsPerWeek = resistanceSets.toFloat() / weeksInPeriod.toFloat()

        val compoundCount = workingSets.count { it.category == CategoryEnum.COMPOUND }
        val isolationCount = workingSets.count { it.category == CategoryEnum.ISOLATION }

        return GoalFitContext(
            sessionsPerWeek = sessionsPerWeek,
            workingSetCount = workingSets.size,
            setsPerWeekPerGroup = setsPerWeekPerGroup,
            sessionsPerWeekPerGroup = sessionsPerWeekPerGroup,
            repRangeBuckets = repRangeBuckets,
            topSetReps = topSets.map { it.reps },
            topSetIntensityShares = topSetIntensityShares,
            topSetHeavyShareAt85 = topSetHeavyShareAt85,
            e1RMTrendByExercise = e1RMTrendByExercise,
            weeklyVolumeSeries = weeklyVolumeSeries,
            compoundLiftPresence = compoundLiftPresence,
            resistanceSetsPerWeek = resistanceSetsPerWeek,
            compoundWorkingSetCount = compoundCount,
            isolationWorkingSetCount = isolationCount,
            topMuscleGroups = topMuscleGroups,
            topExerciseExampleIds = topExerciseExampleIds,
            strengthShare = strengthShare,
            hypertrophyShare = hypertrophyShare,
            enduranceShare = enduranceShare,
        )
    }

    private fun extractWorkingSets(perSession: List<SessionStats>): List<WorkingSet> =
        perSession.flatMap { (training, statsList) ->
            statsList.flatMap { stats ->
                stats.exercise.iterations.mapNotNull { iter ->
                    val w = iter.volume
                    val r = iter.repetitions
                    if (!w.isFinite() || w <= 0f || r <= 0) return@mapNotNull null
                    val external = (iter.externalWeight ?: 0f) +
                            (iter.extraWeight ?: 0f) -
                            (iter.assistWeight ?: 0f)
                    WorkingSet(
                        trainingId = training.id,
                        trainingDate = training.createdAt.date,
                        exampleId = stats.exampleId,
                        category = stats.exercise.exerciseExample.category,
                        weight = w,
                        reps = r,
                        externalLoad = external,
                    )
                }
            }
        }

    /** One top set per (training, exercise) — strictly the heaviest heavy-attempt set. */
    private fun pickTopSets(workingSets: List<WorkingSet>): List<WorkingSet> = workingSets
        .groupBy { it.trainingId to it.exampleId }
        .values
        .mapNotNull { sets ->
            sets.filter { it.reps in 1..TOP_SET_HEAVY_REPS_MAX }
                .maxByOrNull { it.weight }
        }

    private fun computeWeeksInPeriod(trainings: List<Training>): Int {
        if (trainings.isEmpty()) return 1
        val dates = trainings.map { it.createdAt.date }
        val min = dates.minOrNull() ?: return 1
        val max = dates.maxOrNull() ?: return 1
        val days = min.daysUntil(max) + 1
        return ceil(days.toFloat() / 7f).toInt().coerceAtLeast(1)
    }

    private fun computeSetsPerWeekPerGroup(
        workingSets: List<WorkingSet>,
        examples: Map<String, ExerciseExample>,
        weeks: Int,
    ): Map<MuscleGroupEnum, Float> {
        if (weeks <= 0) return emptyMap()
        val totals = mutableMapOf<MuscleGroupEnum, Float>()
        workingSets.forEach { set ->
            val groupShares = exampleGroupContributions(examples[set.exampleId]) ?: return@forEach
            groupShares.forEach { (group, share) ->
                if (share <= 0f) return@forEach
                totals[group] = (totals[group] ?: 0f) + share
            }
        }
        return totals.mapValues { (_, sets) -> sets / weeks.toFloat() }
    }

    private fun computeSessionsPerWeekPerGroup(
        workingSets: List<WorkingSet>,
        examples: Map<String, ExerciseExample>,
        weeks: Int,
    ): Map<MuscleGroupEnum, Float> {
        if (weeks <= 0) return emptyMap()
        val groupSessions = mutableMapOf<MuscleGroupEnum, MutableSet<String>>()
        workingSets.forEach { set ->
            val groupShares = exampleGroupContributions(examples[set.exampleId]) ?: return@forEach
            groupShares.forEach { (group, _) ->
                groupSessions.getOrPut(group) { mutableSetOf() }.add(set.trainingId)
            }
        }
        return groupSessions.mapValues { (_, sessions) ->
            sessions.size.toFloat() / weeks.toFloat()
        }
    }

    /**
     * Per-group set credit — bundle shares are aggregated to the group level
     * first; ≥[AGONIST_GROUP_THRESHOLD] credits 1.0 set (agonist),
     * ≥[SYNERGIST_GROUP_THRESHOLD] credits 0.5 set (synergist), below that
     * drops as incidental.
     */
    private fun exampleGroupContributions(
        example: ExerciseExample?,
    ): Map<MuscleGroupEnum, Float>? {
        if (example == null) return null
        val bundles = example.bundles
        if (bundles.isEmpty()) return null
        val totalShare = bundles.sumOf { it.percentage.coerceAtLeast(0) }
        if (totalShare <= 0) return null

        val groupShare = mutableMapOf<MuscleGroupEnum, Float>()
        bundles.forEach { bundle ->
            val share = bundle.percentage.coerceAtLeast(0).toFloat()
            if (share <= 0f) return@forEach
            val group = bundle.muscle.type.group()
            groupShare[group] = (groupShare[group] ?: 0f) + share / totalShare.toFloat()
        }

        val out = mutableMapOf<MuscleGroupEnum, Float>()
        groupShare.forEach { (group, share) ->
            val credit = when {
                share >= AGONIST_GROUP_THRESHOLD -> AGONIST_SET_WEIGHT
                share >= SYNERGIST_GROUP_THRESHOLD -> SYNERGIST_SET_WEIGHT
                else -> 0f
            }
            if (credit > 0f) out[group] = credit
        }
        return out.takeIf { it.isNotEmpty() }
    }

    private fun computeTopSetIntensityShares(
        topSets: List<WorkingSet>,
        e1RMByExercise: Map<String, Float>,
    ): Map<IntensityBand, Float> {
        if (topSets.isEmpty()) return emptyMap()
        var heavy = 0
        var moderate = 0
        var light = 0
        var counted = 0
        topSets.forEach { ts ->
            val e1rm = e1RMByExercise[ts.exampleId] ?: return@forEach
            if (e1rm <= EPS) return@forEach
            val frac = ts.weight / e1rm
            counted += 1
            when {
                frac >= HEAVY_FRACTION -> heavy += 1
                frac >= MODERATE_FRACTION -> moderate += 1
                else -> light += 1
            }
        }
        if (counted == 0) return emptyMap()
        val denom = counted.toFloat()
        return mapOf(
            IntensityBand.HEAVY_80_PLUS to (heavy / denom) * 100f,
            IntensityBand.MODERATE_60_79 to (moderate / denom) * 100f,
            IntensityBand.LIGHT_BELOW_60 to (light / denom) * 100f,
        )
    }

    /** % of top sets at or above [threshold] (fraction of e1RM). Null when nothing was counted. */
    private fun computeIntensityShareAt(
        topSets: List<WorkingSet>,
        e1RMByExercise: Map<String, Float>,
        threshold: Float,
    ): Float? {
        if (topSets.isEmpty()) return null
        var matched = 0
        var counted = 0
        topSets.forEach { ts ->
            val e1rm = e1RMByExercise[ts.exampleId] ?: return@forEach
            if (e1rm <= EPS) return@forEach
            counted += 1
            if (ts.weight / e1rm >= threshold) matched += 1
        }
        if (counted == 0) return null
        return (matched.toFloat() / counted.toFloat()) * 100f
    }

    private fun computeE1RMTrendByExercise(workingSets: List<WorkingSet>): Map<String, Float> {
        if (workingSets.isEmpty()) return emptyMap()
        val out = mutableMapOf<String, Float>()

        workingSets.groupBy { it.exampleId }.forEach { (exampleId, sets) ->
            val byDate = sets.groupBy { it.trainingDate }
            if (byDate.size < E1RM_TREND_MIN_SESSIONS) return@forEach

            val firstDate = byDate.keys.min()
            val points: List<Pair<Float, Float>> = byDate.entries
                .sortedBy { it.key }
                .map { (date, sessionSets) ->
                    val days = firstDate.daysUntil(date).toFloat()
                    val e1rm =
                        estimateExerciseOneRm(sessionSets.map { LiftSet(it.weight, it.reps) })
                    days to e1rm
                }
                .filter { it.second > EPS }

            if (points.size < E1RM_TREND_MIN_SESSIONS) return@forEach

            val slope = theilSenSlope(points) ?: return@forEach
            val firstY = points.first().second
            val totalDays = points.last().first - points.first().first
            if (totalDays <= EPS || firstY <= EPS) return@forEach

            val trend = (slope * totalDays) / firstY
            if (trend.isFinite()) out[exampleId] = trend
        }
        return out
    }

    /** Median of pairwise slopes — null when no pair has a non-zero x-delta. */
    private fun theilSenSlope(points: List<Pair<Float, Float>>): Float? {
        if (points.size < 2) return null
        val slopes = mutableListOf<Float>()
        for (i in points.indices) {
            for (j in i + 1 until points.size) {
                val (xi, yi) = points[i]
                val (xj, yj) = points[j]
                val dx = xj - xi
                if (dx > EPS) slopes.add((yj - yi) / dx)
            }
        }
        if (slopes.isEmpty()) return null
        slopes.sort()
        val n = slopes.size
        return if (n % 2 == 1) slopes[n / 2]
        else (slopes[n / 2 - 1] + slopes[n / 2]) / 2f
    }

    /**
     * Sliding 7-day stimulus series anchored on the user's first training day.
     * Includes zero gap weeks — a missed week is real signal for stability rules.
     */
    private fun computeWeeklyVolumeSeries(workingSets: List<WorkingSet>): List<Float> {
        if (workingSets.isEmpty()) return emptyList()
        val firstDate = workingSets.minOf { it.trainingDate }
        val perWeek = mutableMapOf<Int, Float>()
        var maxWeekIndex = 0
        workingSets.forEach { set ->
            val daysFromStart = firstDate.daysUntil(set.trainingDate).coerceAtLeast(0)
            val weekIndex = daysFromStart / 7
            val s = set.stimulus
            if (!s.isFinite() || s <= 0f) return@forEach
            perWeek[weekIndex] = (perWeek[weekIndex] ?: 0f) + s
            if (weekIndex > maxWeekIndex) maxWeekIndex = weekIndex
        }
        return (0..maxWeekIndex).map { perWeek[it] ?: 0f }
    }

    private fun computeCompoundLiftPresence(
        workingSets: List<WorkingSet>,
        examples: Map<String, ExerciseExample>,
    ): Map<CompoundLiftKind, String> {
        if (workingSets.isEmpty()) return emptyMap()
        val out = mutableMapOf<CompoundLiftKind, String>()
        workingSets.distinctBy { it.exampleId }.forEach { set ->
            val example = examples[set.exampleId] ?: return@forEach
            val kind = resolveCompoundLiftKind(example) ?: return@forEach
            if (kind !in out) out[kind] = set.exampleId
        }
        return out
    }

    /**
     * Structural compound-lift resolver — uses [ForceTypeEnum] + [CategoryEnum]
     * + dominant muscle group, locale-independent across custom exercises.
     */
    private fun resolveCompoundLiftKind(example: ExerciseExample?): CompoundLiftKind? {
        if (example == null) return null
        val value = example.value

        if (value.category != CategoryEnum.COMPOUND) return null

        val dominantGroup = example.bundles
            .filter { it.percentage > 0 }
            .maxByOrNull { it.percentage }
            ?.muscle?.type?.group()
            ?: return null

        return when (value.forceType) {
            ForceTypeEnum.HINGE -> {
                val hasBackWork = example.bundles.any {
                    it.percentage > 0 && it.muscle.type.group() == MuscleGroupEnum.BACK_MUSCLES
                }
                if (hasBackWork) CompoundLiftKind.DEADLIFT else null
            }

            ForceTypeEnum.PULL ->
                if (dominantGroup == MuscleGroupEnum.BACK_MUSCLES) CompoundLiftKind.ROW else null

            ForceTypeEnum.PUSH -> when (dominantGroup) {
                MuscleGroupEnum.LEGS -> CompoundLiftKind.SQUAT
                MuscleGroupEnum.CHEST_MUSCLES -> CompoundLiftKind.BENCH_PRESS
                MuscleGroupEnum.SHOULDER_MUSCLES -> CompoundLiftKind.OVERHEAD_PRESS
                else -> null
            }
        }
    }

    private fun populateStimulus(
        workingSets: List<WorkingSet>,
        e1RMByExercise: Map<String, Float>,
    ): List<WorkingSet> = workingSets.map { set ->
        val e1rm = e1RMByExercise[set.exampleId] ?: 0f
        val pct = if (e1rm > EPS) (set.weight / e1rm).coerceIn(0f, 1.2f) else Float.NaN
        val s = setStimulus(set.weight, set.reps, pct)
        set.copy(stimulus = if (s.isFinite() && s > 0f) s else 0f)
    }

    // -------------------------------------------------------------------------
    // Goal-fit diagnostic — per-goal rule matrix
    //
    // Each evaluator emits one finding per evaluable rule. Rules with
    // insufficient data are omitted (no synthetic PASS/FAIL).
    // -------------------------------------------------------------------------

    private fun evaluateBuildMuscle(ctx: GoalFitContext): List<GoalFitFinding> {
        val findings = mutableListOf<GoalFitFinding>()

        findings += atLeastFinding(
            rule = GoalFitRule.BUILD_MUSCLE_HYPERTROPHY_REP_RANGE_SHARE,
            actual = repBucketSharePercent(ctx, RepBucket.HYPERTROPHY_6_15),
            passMin = HYPERTROPHY_REP_RANGE_PASS_MIN,
            warnMin = HYPERTROPHY_REP_RANGE_WARN_MIN,
        )

        MuscleGroupEnum.entries.forEach { group ->
            val sets = ctx.setsPerWeekPerGroup[group] ?: 0f
            findings += primaryGroupSetsFinding(actual = sets, context = listOf(group.name))
        }

        MuscleGroupEnum.entries.forEach { group ->
            val freq = ctx.sessionsPerWeekPerGroup[group] ?: 0f
            findings += atLeastFinding(
                rule = GoalFitRule.BUILD_MUSCLE_WEEKLY_FREQUENCY_PER_PRIMARY_GROUP,
                actual = freq,
                passMin = PRIMARY_FREQUENCY_PASS_MIN,
                warnMin = PRIMARY_FREQUENCY_WARN_MIN,
                context = listOf(group.name),
            )
        }

        accessoryInversionFinding(ctx)?.let(findings::add)

        val trendPool = ctx.topExerciseExampleIds.mapNotNull { ctx.e1RMTrendByExercise[it] }
        if (trendPool.isNotEmpty()) {
            findings += atLeastFinding(
                rule = GoalFitRule.BUILD_MUSCLE_LOAD_PROGRESSION,
                actual = medianFloat(trendPool),
                passMin = LOAD_PROGRESSION_PASS_MIN,
                warnMin = LOAD_PROGRESSION_WARN_MIN,
            )
        }

        return findings
    }

    private fun primaryGroupSetsFinding(actual: Float, context: List<String>): GoalFitFinding {
        val severity = when {
            actual >= PRIMARY_SETS_PASS_MIN && actual <= PRIMARY_SETS_PASS_MAX -> GoalFitSeverity.PASS
            actual >= PRIMARY_SETS_WARN_LOW_MIN && actual <= PRIMARY_SETS_WARN_HIGH_MAX -> GoalFitSeverity.WARN
            else -> GoalFitSeverity.FAIL
        }
        return GoalFitFinding(
            rule = GoalFitRule.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
            severity = severity,
            actualValue = actual,
            targetMin = PRIMARY_SETS_PASS_MIN,
            targetMax = PRIMARY_SETS_PASS_MAX,
            context = context,
        )
    }

    /**
     * Returns null when no muscle-group share data is available — emitting a
     * synthetic PASS in that case would mislead the user.
     */
    private fun accessoryInversionFinding(ctx: GoalFitContext): GoalFitFinding? {
        val byGroup: Map<MuscleGroupEnum, Int> = ctx.topMuscleGroups
            .associate { it.group to it.share.coerceAtLeast(0) }
        if (byGroup.isEmpty()) return null

        val maxPrimary = PRIMARY_GROUPS.maxOf { byGroup[it] ?: 0 }
        val invertedAccessories = ACCESSORY_GROUPS
            .filter { (byGroup[it] ?: 0) > maxPrimary }
            .map { it.name }

        val severity =
            if (invertedAccessories.isEmpty()) GoalFitSeverity.PASS else GoalFitSeverity.FAIL
        return GoalFitFinding(
            rule = GoalFitRule.BUILD_MUSCLE_ACCESSORY_INVERSION,
            severity = severity,
            actualValue = invertedAccessories.size.toFloat(),
            targetMin = null,
            targetMax = 0f,
            context = invertedAccessories,
        )
    }

    private fun evaluateGetStronger(ctx: GoalFitContext): List<GoalFitFinding> {
        val findings = mutableListOf<GoalFitFinding>()

        val presentCount = ctx.compoundLiftPresence.size.toFloat()
        findings += GoalFitFinding(
            rule = GoalFitRule.GET_STRONGER_COMPOUND_PRESENCE,
            severity = when {
                presentCount >= COMPOUND_PRESENCE_PASS_MIN -> GoalFitSeverity.PASS
                presentCount >= COMPOUND_PRESENCE_WARN_MIN -> GoalFitSeverity.WARN
                else -> GoalFitSeverity.FAIL
            },
            actualValue = presentCount,
            targetMin = COMPOUND_PRESENCE_PASS_MIN,
            targetMax = CompoundLiftKind.entries.size.toFloat(),
        )

        findings += atLeastFinding(
            rule = GoalFitRule.GET_STRONGER_HEAVY_INTENSITY_SHARE,
            actual = ctx.topSetIntensityShares[IntensityBand.HEAVY_80_PLUS] ?: 0f,
            passMin = HEAVY_INTENSITY_PASS_MIN,
            warnMin = HEAVY_INTENSITY_WARN_MIN,
        )

        if (ctx.topSetReps.isNotEmpty()) {
            val lowRepCount = ctx.topSetReps.count { it in 1..5 }
            val pct = (lowRepCount.toFloat() / ctx.topSetReps.size.toFloat()) * 100f
            findings += atLeastFinding(
                rule = GoalFitRule.GET_STRONGER_STRENGTH_REP_RANGE_SHARE,
                actual = pct,
                passMin = STRENGTH_REP_RANGE_PASS_MIN,
                warnMin = STRENGTH_REP_RANGE_WARN_MIN,
            )
        }

        // Strict: skip the rule when the user did no isolation work — there is
        // no meaningful ratio to score and an "infinite" placeholder distorts
        // the breakdown card.
        val compound = ctx.compoundWorkingSetCount
        val isolation = ctx.isolationWorkingSetCount
        if (compound > 0 && isolation > 0) {
            findings += atLeastFinding(
                rule = GoalFitRule.GET_STRONGER_COMPOUND_TO_ISOLATION_RATIO,
                actual = compound.toFloat() / isolation.toFloat(),
                passMin = COMPOUND_TO_ISOLATION_PASS_MIN,
                warnMin = COMPOUND_TO_ISOLATION_WARN_MIN,
            )
        }

        val trendPool = ctx.compoundLiftPresence.values.mapNotNull { ctx.e1RMTrendByExercise[it] }
        if (trendPool.isNotEmpty()) {
            findings += atLeastFinding(
                rule = GoalFitRule.GET_STRONGER_STRENGTH_PROGRESSION,
                actual = medianFloat(trendPool),
                passMin = STRENGTH_PROGRESSION_PASS_MIN,
                warnMin = STRENGTH_PROGRESSION_WARN_MIN,
            )
        }

        return findings
    }

    private fun evaluateMaintain(ctx: GoalFitContext): List<GoalFitFinding> {
        val findings = mutableListOf<GoalFitFinding>()

        val sH = ctx.strengthShare
        val hH = ctx.hypertrophyShare
        if (sH + hH > 0) {
            val skew = maxOf(sH, hH).toFloat() / (sH + hH).toFloat()
            findings += atMostFinding(
                rule = GoalFitRule.MAINTAIN_WORK_BALANCE_SKEW,
                actual = skew,
                passMax = WORK_BALANCE_SKEW_PASS_MAX,
                warnMax = WORK_BALANCE_SKEW_WARN_MAX,
            )
        }

        findings += groupCoverageFinding(ctx, GoalFitRule.MAINTAIN_GROUP_COVERAGE)

        findings += atLeastFinding(
            rule = GoalFitRule.MAINTAIN_WEEKLY_FREQUENCY,
            actual = ctx.sessionsPerWeek,
            passMin = WEEKLY_FREQUENCY_PASS_MIN,
            warnMin = WEEKLY_FREQUENCY_WARN_MIN,
        )

        if (ctx.weeklyVolumeSeries.size >= 2) {
            volumeStabilityRatio(ctx.weeklyVolumeSeries)?.let { stability ->
                findings += atMostFinding(
                    rule = GoalFitRule.MAINTAIN_WEEKLY_VOLUME_STABILITY,
                    actual = stability,
                    passMax = WEEKLY_VOLUME_STABILITY_PASS_MAX,
                    warnMax = WEEKLY_VOLUME_STABILITY_WARN_MAX,
                )
            }
        }

        return findings
    }

    private fun evaluateGeneralFitness(ctx: GoalFitContext): List<GoalFitFinding> {
        val findings = mutableListOf<GoalFitFinding>()

        val present = listOf(ctx.strengthShare, ctx.hypertrophyShare, ctx.enduranceShare)
            .count { it >= QUALITY_VARIETY_THRESHOLD_PERCENT }
            .toFloat()
        findings += GoalFitFinding(
            rule = GoalFitRule.GENERAL_FITNESS_QUALITY_VARIETY,
            severity = when {
                present >= QUALITY_VARIETY_PASS_MIN -> GoalFitSeverity.PASS
                present >= QUALITY_VARIETY_WARN_MIN -> GoalFitSeverity.WARN
                else -> GoalFitSeverity.FAIL
            },
            actualValue = present,
            targetMin = QUALITY_VARIETY_PASS_MIN,
            targetMax = 3f,
        )

        findings += groupCoverageFinding(ctx, GoalFitRule.GENERAL_FITNESS_GROUP_COVERAGE)

        findings += atLeastFinding(
            rule = GoalFitRule.GENERAL_FITNESS_WEEKLY_FREQUENCY,
            actual = ctx.sessionsPerWeek,
            passMin = WEEKLY_FREQUENCY_PASS_MIN,
            warnMin = WEEKLY_FREQUENCY_WARN_MIN,
        )

        val sH = ctx.strengthShare
        val eH = ctx.enduranceShare
        if (sH + eH > 0) {
            val skew = maxOf(sH, eH).toFloat() / (sH + eH).toFloat()
            findings += atMostFinding(
                rule = GoalFitRule.GENERAL_FITNESS_WORK_BALANCE_SKEW,
                actual = skew,
                passMax = WORK_BALANCE_SKEW_PASS_MAX,
                warnMax = WORK_BALANCE_SKEW_WARN_MAX,
            )
        }

        return findings
    }

    private fun groupCoverageFinding(ctx: GoalFitContext, rule: GoalFitRule): GoalFitFinding {
        val coverageCount = ctx.topMuscleGroups.count { it.share > 0 }.toFloat()
        val severity = when {
            coverageCount >= GROUP_COVERAGE_PASS_MIN -> GoalFitSeverity.PASS
            coverageCount >= GROUP_COVERAGE_WARN_MIN -> GoalFitSeverity.WARN
            else -> GoalFitSeverity.FAIL
        }
        return GoalFitFinding(
            rule = rule,
            severity = severity,
            actualValue = coverageCount,
            targetMin = GROUP_COVERAGE_PASS_MIN,
            targetMax = MuscleGroupEnum.entries.size.toFloat(),
        )
    }

    private fun evaluateLoseFat(ctx: GoalFitContext): List<GoalFitFinding> {
        val findings = mutableListOf<GoalFitFinding>()

        findings += atLeastFinding(
            rule = GoalFitRule.LOSE_FAT_SESSION_ADHERENCE,
            actual = ctx.sessionsPerWeek,
            passMin = LOSE_FAT_SESSION_PASS_MIN,
            warnMin = LOSE_FAT_SESSION_WARN_MIN,
        )

        findings += atLeastFinding(
            rule = GoalFitRule.LOSE_FAT_RESISTANCE_PRESENCE,
            actual = ctx.resistanceSetsPerWeek,
            passMin = LOSE_FAT_RESISTANCE_PASS_MIN,
            warnMin = LOSE_FAT_RESISTANCE_WARN_MIN,
        )

        if (ctx.weeklyVolumeSeries.size >= 2) {
            val first = ctx.weeklyVolumeSeries.firstOrNull { it > 0f }
            val last = ctx.weeklyVolumeSeries.lastOrNull { it > 0f }
            if (first != null && last != null && first > 0f) {
                findings += atLeastFinding(
                    rule = GoalFitRule.LOSE_FAT_WEEKLY_VOLUME_TREND,
                    actual = last / first,
                    passMin = LOSE_FAT_VOLUME_TREND_PASS_MIN,
                    warnMin = LOSE_FAT_VOLUME_TREND_WARN_MIN,
                )
            }
        }

        return findings
    }

    private fun evaluateReturnToTraining(ctx: GoalFitContext): List<GoalFitFinding> {
        val findings = mutableListOf<GoalFitFinding>()

        val freq = ctx.sessionsPerWeek
        findings += GoalFitFinding(
            rule = GoalFitRule.RETURN_TO_TRAINING_WEEKLY_FREQUENCY,
            severity = if (freq in RTT_FREQUENCY_PASS_MIN..RTT_FREQUENCY_PASS_MAX) {
                GoalFitSeverity.PASS
            } else {
                GoalFitSeverity.WARN
            },
            actualValue = freq,
            targetMin = RTT_FREQUENCY_PASS_MIN,
            targetMax = RTT_FREQUENCY_PASS_MAX,
        )

        if (ctx.weeklyVolumeSeries.size >= 2) {
            val median = medianFloat(ctx.weeklyVolumeSeries)
            val maxWeekly = ctx.weeklyVolumeSeries.maxOrNull()
            if (median > 0f && maxWeekly != null) {
                findings += atMostFinding(
                    rule = GoalFitRule.RETURN_TO_TRAINING_LOAD_RAMP_GENTLE,
                    actual = maxWeekly / median,
                    passMax = RTT_LOAD_RAMP_PASS_MAX,
                    warnMax = RTT_LOAD_RAMP_WARN_MAX,
                )
            }
        }

        // Strict ≥85% e1RM share — skip when no top set could be evaluated.
        ctx.topSetHeavyShareAt85?.let { share ->
            findings += atMostFinding(
                rule = GoalFitRule.RETURN_TO_TRAINING_INTENSITY_FLOOR,
                actual = share,
                passMax = RTT_INTENSITY_FLOOR_PASS_MAX,
                warnMax = RTT_INTENSITY_FLOOR_WARN_MAX,
            )
        }

        return findings
    }

    private fun atLeastFinding(
        rule: GoalFitRule,
        actual: Float,
        passMin: Float,
        warnMin: Float,
        context: List<String> = emptyList(),
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

    private fun atMostFinding(
        rule: GoalFitRule,
        actual: Float,
        passMax: Float,
        warnMax: Float,
        context: List<String> = emptyList(),
    ): GoalFitFinding {
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
            context = context,
        )
    }

    private fun repBucketSharePercent(ctx: GoalFitContext, bucket: RepBucket): Float {
        val total = ctx.workingSetCount
        if (total <= 0) return 0f
        val n = ctx.repRangeBuckets[bucket] ?: 0
        return (n.toFloat() / total.toFloat()) * 100f
    }

    /**
     * (max − min) / mean of [series] — null when the series has fewer than two
     * non-zero weeks (the "did the user even train?" question belongs to the
     * adherence rule, not the stability rule).
     */
    private fun volumeStabilityRatio(series: List<Float>): Float? {
        if (series.size < 2) return null
        if (series.count { it > 0f } < 2) return null
        val max = series.maxOrNull() ?: return null
        val min = series.minOrNull() ?: return null
        val mean = series.average().toFloat()
        if (mean <= 0f || !mean.isFinite()) return null
        val r = (max - min) / mean
        return if (r.isFinite()) r else null
    }

    // -------------------------------------------------------------------------
    // Math helpers
    // -------------------------------------------------------------------------

    private fun setStimulus(weight: Float, reps: Int, pct: Float): Float {
        val base = baseSetStimulus(weight, reps)
        if (base <= EPS) return 0f
        if (!pct.isFinite()) return base

        val p = pct.coerceIn(0f, 1.2f)
        val t = (p - PCT_BOOST_CENTER) / PCT_BOOST_HALFWIDTH
        val boost = PCT_BOOST_MIN + PCT_BOOST_RANGE * smoothStep(t)

        val v = base * boost
        return if (v.isFinite() && v > EPS) v else 0f
    }

    private fun baseSetStimulus(weight: Float, reps: Int): Float {
        val r = reps.coerceAtLeast(0)
        if (r == 0) return 0f
        val w = weight.coerceAtLeast(0f)

        val repsTerm = sqrt(r.toFloat())
        val weightTerm = ln(1.0 + w.toDouble()).toFloat()
        val intensity = 1f + (weightTerm / (STIMULUS_LN_WEIGHT_SCALE * STIMULUS_LN_WEIGHT_FRACTION))

        val v = repsTerm * intensity
        return if (v.isFinite() && v > EPS) v else 0f
    }

    private fun estimateExerciseOneRm(sets: List<LiftSet>): Float {
        val working = pickWorkingSets(sets)
        val e1rms = working
            .map { epleyE1rm(it.weight, it.reps) }
            .filter { it.isFinite() && it > EPS }
            .sorted()
        if (e1rms.isEmpty()) return 0f
        val p = percentileSorted(e1rms, HEAVY_E1RM_PERCENTILE)
        return if (p.isFinite() && p > EPS) p else 0f
    }

    /**
     * Strict working-set picker — returns sets at or above
     * [WORKING_MIN_WEIGHT_FRACTION] of the reference (median of the top-3
     * heaviest weights). When the user did not perform [WORKING_MIN_SETS]
     * qualifying sets this returns an empty list — callers must omit the
     * metric rather than infer a 1RM from warm-ups.
     */
    private fun pickWorkingSets(sets: List<LiftSet>): List<LiftSet> {
        if (sets.isEmpty()) return emptyList()
        val weights = sets.asSequence()
            .map { it.weight }
            .filter { it.isFinite() && it > EPS }
            .toList()
        if (weights.isEmpty()) return emptyList()

        val topCount = min(WORKING_REFERENCE_TOP_SETS, weights.size)
        val topAsc = weights.sortedDescending().take(topCount).sorted()
        val ref = medianSorted(topAsc)
        if (!ref.isFinite() || ref <= EPS) return emptyList()

        val threshold = ref * WORKING_MIN_WEIGHT_FRACTION
        val picked = sets.filter { it.weight.isFinite() && it.weight >= threshold }
        return if (picked.size >= WORKING_MIN_SETS) picked else emptyList()
    }

    private fun epleyE1rm(weight: Float, reps: Int): Float {
        val r = reps.coerceIn(1, ONE_RM_REPS_CAP)
        val mult = 1f + ((r - 1).toFloat() / 30f)
        val v = weight * mult
        return if (v.isFinite() && v > 0f) v else 0f
    }

    private fun medianFloat(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val n = sorted.size
        val mid = n / 2
        return if (n % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2f
    }

    private fun medianSorted(sortedAsc: List<Float>): Float {
        val n = sortedAsc.size
        if (n == 0) return 0f
        if (n == 1) return sortedAsc[0]
        val mid = n / 2
        return if (n % 2 == 1) {
            sortedAsc[mid]
        } else {
            val a = sortedAsc[mid - 1]
            val b = sortedAsc[mid]
            val v = a + (b - a) * 0.5f
            if (v.isFinite()) v else a
        }
    }

    private fun percentileSorted(sorted: List<Float>, q: Float): Float {
        val n = sorted.size
        if (n == 0) return 0f
        if (n == 1) return sorted[0]

        val qq = q.coerceIn(0f, 1f)
        val idx = (qq * (n - 1)).toDouble()
        val i0 = idx.toInt().coerceIn(0, n - 1)
        val i1 = (i0 + 1).coerceIn(0, n - 1)
        val t = (idx - i0.toDouble()).toFloat()

        val a = sorted[i0]
        val b = sorted[i1]
        val v = a + (b - a) * t
        return if (v.isFinite()) v else a
    }

    private fun smoothStep(x01: Float): Float {
        val x = x01.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    private fun clamp01(v: Float): Float = if (v.isFinite()) v.coerceIn(0f, 1f) else 0f

    private fun safe(v: Float): Float = if (v.isFinite() && v > 0f) v else 0f

    private fun log1pSafe(v: Float): Float {
        if (!v.isFinite() || v <= 0f) return 0f
        return ln(1.0 + v.toDouble()).toFloat()
    }

    // -------------------------------------------------------------------------
    // Internal types
    // -------------------------------------------------------------------------

    private data class Analysis(
        val raw: Raw,
        val sessionCount: Int,
        val strengthShare: Int,
        val hypertrophyShare: Int,
        val enduranceShare: Int,
        val compoundRatio: Int,
        val topExercises: List<TopExerciseContribution>,
        val topMuscles: List<TopMuscleContribution>,
        val topMuscleGroups: List<TopMuscleGroupContribution>,
        val findings: List<GoalFitFinding>,
    ) {
        val isSilent: Boolean
            get() = raw.totalWork <= EPS ||
                    (strengthShare + hypertrophyShare + enduranceShare) == 0

        companion object {
            val EMPTY = Analysis(
                raw = Raw.ZERO,
                sessionCount = 0,
                strengthShare = 0,
                hypertrophyShare = 0,
                enduranceShare = 0,
                compoundRatio = 0,
                topExercises = emptyList(),
                topMuscles = emptyList(),
                topMuscleGroups = emptyList(),
                findings = emptyList(),
            )
        }
    }

    private data class Raw(
        val strengthShare: Float,
        val hypertrophyShare: Float,
        val enduranceShare: Float,
        val compoundRatio: Float,
        val freeWeightRatio: Float,
        val specializationTop2Percent: Float,
        val activity: Float,
        val totalWork: Float,
    ) {
        companion object {
            val ZERO = Raw(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        }
    }

    private data class DimensionWork(
        val strength: Float,
        val hypertrophy: Float,
        val endurance: Float,
    )

    private data class SessionStats(
        val training: Training,
        val stats: List<ExerciseStats>,
    )

    private data class ExerciseStats(
        val exercise: Exercise,
        val exampleId: String,
        val sets: List<SetStats>,
        val stimulus: Float,
    )

    private data class SetStats(
        val weight: Float,
        val reps: Int,
        val pct: Float,
        val stimulus: Float,
    )

    private data class LiftSet(
        val weight: Float,
        val reps: Int,
    )

    private data class WorkingSet(
        val trainingId: String,
        val trainingDate: LocalDate,
        val exampleId: String,
        val category: CategoryEnum?,
        val weight: Float,
        val reps: Int,
        val externalLoad: Float,
        val stimulus: Float = 0f,
    ) {
        fun repBucket(): RepBucket = when {
            reps <= 5 -> RepBucket.STRENGTH_1_5
            reps <= 15 -> RepBucket.HYPERTROPHY_6_15
            else -> RepBucket.ENDURANCE_16_PLUS
        }
    }

    private data class GoalFitContext(
        val sessionsPerWeek: Float,
        val workingSetCount: Int,
        val setsPerWeekPerGroup: Map<MuscleGroupEnum, Float>,
        val sessionsPerWeekPerGroup: Map<MuscleGroupEnum, Float>,
        val repRangeBuckets: Map<RepBucket, Int>,
        val topSetReps: List<Int>,
        val topSetIntensityShares: Map<IntensityBand, Float>,
        /** % of top sets at ≥85% e1RM. Null when no top set could be evaluated. */
        val topSetHeavyShareAt85: Float?,
        val e1RMTrendByExercise: Map<String, Float>,
        val weeklyVolumeSeries: List<Float>,
        val compoundLiftPresence: Map<CompoundLiftKind, String>,
        val resistanceSetsPerWeek: Float,
        val compoundWorkingSetCount: Int,
        val isolationWorkingSetCount: Int,
        val topMuscleGroups: List<TopMuscleGroupContribution>,
        val topExerciseExampleIds: List<String>,
        val strengthShare: Int,
        val hypertrophyShare: Int,
        val enduranceShare: Int,
    )

    private enum class RepBucket { STRENGTH_1_5, HYPERTROPHY_6_15, ENDURANCE_16_PLUS }

    private enum class IntensityBand { HEAVY_80_PLUS, MODERATE_60_79, LIGHT_BELOW_60 }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private companion object {
        private const val EPS: Float = 1e-3f

        // Score weighting
        private const val SPEC_CORE_WEIGHT = 0.70f
        private const val SPEC_QUALITY_WEIGHT = 0.30f
        private const val ACTIVITY_QUALITY_SATURATION = 3f
        private const val SPECIALIZATION_SATURATION_PERCENT = 80f
        private const val SUPPRESSED_AXIS_PENALTY_FULL = 0.50f

        // RTT scoring band
        private const val RTT_WORK_FLOOR = 20f
        private const val RTT_WORK_PEAK = 30f
        private const val RTT_WORK_CEILING = 90f

        // Top-N caps for the contribution lists
        private const val TOP_EXERCISES_LIMIT = 5
        private const val TOP_MUSCLES_LIMIT = 30
        private const val LOAD_PROGRESSION_TOP_N = 5

        // Session-length resolution
        private const val MIN_RECORDED_MINUTES = 5f
        private const val MIN_ESTIMATED_MINUTES = 10f
        private const val MAX_ESTIMATED_MINUTES = 180f
        private const val MINUTES_PER_SET_ESTIMATE = 1.4f

        /** Sessions with zero activity get omitted; activity > 0 sessions carry at least this weight. */
        private const val SESSION_WEIGHT_FLOOR = 0.25f

        // Stimulus formula
        private const val STIMULUS_LN_WEIGHT_SCALE = 8f
        private const val STIMULUS_LN_WEIGHT_FRACTION = 0.55f
        private const val PCT_BOOST_MIN = 0.75f
        private const val PCT_BOOST_RANGE = 0.50f
        private const val PCT_BOOST_CENTER = 0.65f
        private const val PCT_BOOST_HALFWIDTH = 0.25f

        // 1RM estimation
        private const val WORKING_MIN_WEIGHT_FRACTION = 0.75f
        private const val WORKING_MIN_SETS = 2
        private const val WORKING_REFERENCE_TOP_SETS = 3
        private const val ONE_RM_REPS_CAP = 12
        private const val HEAVY_E1RM_PERCENTILE = 0.90f

        // Goal-fit input — non-rule constants
        private const val BODYWEIGHT_FLOOR_KG = 5f
        private const val E1RM_TREND_MIN_SESSIONS = 3
        private const val HEAVY_FRACTION = 0.80f
        private const val MODERATE_FRACTION = 0.60f
        private const val RTT_INTENSITY_FLOOR_FRACTION = 0.85f
        private const val AGONIST_GROUP_THRESHOLD = 0.35f
        private const val SYNERGIST_GROUP_THRESHOLD = 0.15f
        private const val AGONIST_SET_WEIGHT = 1.0f
        private const val SYNERGIST_SET_WEIGHT = 0.5f
        private const val TOP_SET_HEAVY_REPS_MAX = 8

        // Goal-fit rule thresholds — PASS / WARN / FAIL boundaries (PASS-side inclusive)
        private const val HYPERTROPHY_REP_RANGE_PASS_MIN = 60f
        private const val HYPERTROPHY_REP_RANGE_WARN_MIN = 40f
        private const val PRIMARY_SETS_PASS_MIN = 10f
        private const val PRIMARY_SETS_PASS_MAX = 20f
        private const val PRIMARY_SETS_WARN_LOW_MIN = 5f
        private const val PRIMARY_SETS_WARN_HIGH_MAX = 25f
        private const val PRIMARY_FREQUENCY_PASS_MIN = 1.5f
        private const val PRIMARY_FREQUENCY_WARN_MIN = 1.0f
        private const val LOAD_PROGRESSION_PASS_MIN = 0.01f
        private const val LOAD_PROGRESSION_WARN_MIN = -0.01f

        private const val COMPOUND_PRESENCE_PASS_MIN = 3f
        private const val COMPOUND_PRESENCE_WARN_MIN = 2f
        private const val HEAVY_INTENSITY_PASS_MIN = 40f
        private const val HEAVY_INTENSITY_WARN_MIN = 20f
        private const val STRENGTH_REP_RANGE_PASS_MIN = 50f
        private const val STRENGTH_REP_RANGE_WARN_MIN = 30f
        private const val COMPOUND_TO_ISOLATION_PASS_MIN = 2.0f
        private const val COMPOUND_TO_ISOLATION_WARN_MIN = 1.0f
        private const val STRENGTH_PROGRESSION_PASS_MIN = 0.03f
        private const val STRENGTH_PROGRESSION_WARN_MIN = 0.0f

        private const val WORK_BALANCE_SKEW_PASS_MAX = 0.70f
        private const val WORK_BALANCE_SKEW_WARN_MAX = 0.85f
        private const val GROUP_COVERAGE_PASS_MIN = 5f
        private const val GROUP_COVERAGE_WARN_MIN = 4f
        private const val WEEKLY_FREQUENCY_PASS_MIN = 2.0f
        private const val WEEKLY_FREQUENCY_WARN_MIN = 1.5f
        private const val WEEKLY_VOLUME_STABILITY_PASS_MAX = 0.30f
        private const val WEEKLY_VOLUME_STABILITY_WARN_MAX = 0.60f

        private const val QUALITY_VARIETY_THRESHOLD_PERCENT = 15
        private const val QUALITY_VARIETY_PASS_MIN = 3f
        private const val QUALITY_VARIETY_WARN_MIN = 2f

        private const val LOSE_FAT_SESSION_PASS_MIN = 3.0f
        private const val LOSE_FAT_SESSION_WARN_MIN = 2.0f
        private const val LOSE_FAT_RESISTANCE_PASS_MIN = 6f
        private const val LOSE_FAT_RESISTANCE_WARN_MIN = 1f
        private const val LOSE_FAT_VOLUME_TREND_PASS_MIN = 0.85f
        private const val LOSE_FAT_VOLUME_TREND_WARN_MIN = 0.70f

        private const val RTT_FREQUENCY_PASS_MIN = 1.0f
        private const val RTT_FREQUENCY_PASS_MAX = 3.0f
        private const val RTT_LOAD_RAMP_PASS_MAX = 1.5f
        private const val RTT_LOAD_RAMP_WARN_MAX = 2.0f
        private const val RTT_INTENSITY_FLOOR_PASS_MAX = 10f
        private const val RTT_INTENSITY_FLOOR_WARN_MAX = 25f

        /** Primary anatomical groups for hypertrophy programming. */
        private val PRIMARY_GROUPS: Set<MuscleGroupEnum> = setOf(
            MuscleGroupEnum.CHEST_MUSCLES,
            MuscleGroupEnum.BACK_MUSCLES,
            MuscleGroupEnum.LEGS,
        )

        /** Accessory groups — small movers that piggy-back on primary work. */
        private val ACCESSORY_GROUPS: Set<MuscleGroupEnum> = setOf(
            MuscleGroupEnum.ARMS_AND_FOREARMS,
            MuscleGroupEnum.SHOULDER_MUSCLES,
            MuscleGroupEnum.ABDOMINAL_MUSCLES,
        )
    }
}
