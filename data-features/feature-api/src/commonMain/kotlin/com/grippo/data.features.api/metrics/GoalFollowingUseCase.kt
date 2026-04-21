package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.GoalPrimaryGoalEnum
import com.grippo.data.features.api.metrics.GoalFollowingUseCase.Companion.SPEC_CORE_WEIGHT
import com.grippo.data.features.api.metrics.GoalFollowingUseCase.Companion.SUPPRESSED_AXIS_PENALTY_FULL
import com.grippo.data.features.api.metrics.models.GoalAdherence
import com.grippo.data.features.api.metrics.models.TopExerciseContribution
import com.grippo.data.features.api.metrics.models.TopMuscleContribution
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

public class GoalFollowingUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val goalFeature: GoalFeature
) {

    private companion object {
        private const val EPS = 1e-3f

        /** Divisor in the `ln(1+weight)` intensity term of setStimulus. */
        private const val STIMULUS_LN_WEIGHT_SCALE = 8f
        private const val STIMULUS_LN_WEIGHT_FRACTION = 0.55f

        private const val WORKING_MIN_WEIGHT_FRACTION = 0.75f
        private const val WORKING_MIN_SETS = 2
        private const val WORKING_REFERENCE_TOP_SETS = 3

        private const val ONE_RM_REPS_CAP = 12
        private const val HEAVY_E1RM_PERCENTILE = 0.90f

        private const val PCT_BOOST_MIN = 0.75f
        private const val PCT_BOOST_RANGE = 0.50f
        private const val PCT_BOOST_CENTER = 0.65f
        private const val PCT_BOOST_HALFWIDTH = 0.25f

        /**
         * Quality weighting inside specialization-style goals (GET_STRONGER /
         * BUILD_MUSCLE / LOSE_FAT). The core axis share provides [SPEC_CORE_WEIGHT]
         * of the score; the remainder is split among quality modifiers.
         */
        private const val SPEC_CORE_WEIGHT = 0.70f
        private const val SPEC_QUALITY_WEIGHT = 0.30f

        /** Saturation point of the activity-density quality signal. */
        private const val ACTIVITY_QUALITY_SATURATION = 3f

        /** Saturation point of the top-2 muscle specialization signal. */
        private const val SPECIALIZATION_SATURATION_PERCENT = 80f

        /**
         * Balanced-dual goals (MAINTAIN, GENERAL_FITNESS) score as
         * `min(axisA, axisB) * 2`. A penalty clamps the score when the
         * suppressed axis grows beyond the threshold below.
         */
        private const val SUPPRESSED_AXIS_PENALTY_FULL = 0.50f

        /**
         * RETURN_TO_TRAINING scoring: adherence peaks in a "low-to-moderate"
         * work band and decays outside it.
         */
        private const val RTT_WORK_FLOOR = 20f
        private const val RTT_WORK_PEAK = 30f
        private const val RTT_WORK_CEILING = 90f

        /**
         * CONSISTENCY is frequency-based: adherence is proportional to the
         * number of valid sessions in the period, saturating at the target
         * count (a light weekly cadence baseline).
         */
        private const val CONSISTENCY_TARGET_SESSIONS = 3

        /** Maximum number of top contributors exposed to consumers. */
        private const val TOP_EXERCISES_LIMIT = 5
        private const val TOP_MUSCLES_LIMIT = 3
    }

    // -------------------------------------------------------------------------
    // Public API — one pipeline, one model per call.
    // -------------------------------------------------------------------------

    public suspend fun fromTrainingsByPrimary(
        trainings: List<Training>,
    ): GoalAdherence? {
        val goal = goalFeature.observeGoal().firstOrNull() ?: return null
        val analysis = analyze(trainings)
        val score = scoreForPrimary(goal.primaryGoal, analysis)
        return analysis.toAdherence(goal = goal, score = score)
    }

    // -------------------------------------------------------------------------
    // Analysis — single pass over the training list.
    // -------------------------------------------------------------------------

    /**
     * Everything the scorer and the breakdown need, computed in one go.
     * Domain-internal: not exposed. [raw] feeds the per-goal formulas;
     * the other fields are surfaced via [GoalAdherence].
     */
    private data class Analysis(
        val raw: Raw,
        val sessionCount: Int,
        val strengthShare: Int,
        val hypertrophyShare: Int,
        val enduranceShare: Int,
        val compoundRatio: Int,
        val topExercises: List<TopExerciseContribution>,
        val topMuscles: List<TopMuscleContribution>,
    ) {
        val isSilent: Boolean
            get() = raw.totalWork <= EPS ||
                    (strengthShare + hypertrophyShare + enduranceShare) == 0
    }

    private suspend fun analyze(trainings: List<Training>): Analysis {
        if (trainings.isEmpty()) return emptyAnalysis()

        val ids = trainings.flatMap { it.exercises }.map { it.exerciseExample.id }.toSet()
        val examples = loadExamples(ids)

        // One pass: build ExerciseStats per session, then reuse for both the
        // per-session Raw (scoring pool) and the period-level artifacts.
        val perSession: List<Pair<Training, List<ExerciseStats>>> = trainings.map { training ->
            training to buildExerciseStats(training.exercises)
        }
        val sessionCount = perSession.count { it.second.isNotEmpty() }

        val raws = perSession.mapNotNull { (training, stats) ->
            if (stats.isEmpty()) null else buildRawFromStats(training, stats, examples)
        }
        if (raws.isEmpty()) return emptyAnalysis().copy(sessionCount = sessionCount)

        val pool = aggregateRaw(raws)
        val strengthShare = (pool.strengthShare * 100f).roundToInt().coerceIn(0, 100)
        val hypertrophyShare = (pool.hypertrophyShare * 100f).roundToInt().coerceIn(0, 100)
        val enduranceShare = (pool.enduranceShare * 100f).roundToInt().coerceIn(0, 100)

        val allStats: List<ExerciseStats> = perSession.flatMap { it.second }
        val totalStimulus = allStats.sumOf { it.stimulus.toDouble() }.toFloat()

        val compoundRatio =
            (weightedCategoryRatioByStimulus(allStats, CategoryEnum.COMPOUND) * 100f)
                .roundToInt().coerceIn(0, 100)

        val muscleTotals = computeMuscleStimulusTotals(allStats, examples)
        val totalMuscle = muscleTotals.values.sumOf { it.toDouble() }.toFloat()
        val topMuscles = muscleTotals.entries
            .filter { it.value.isFinite() && it.value > EPS }
            .sortedByDescending { it.value }
            .take(TOP_MUSCLES_LIMIT)
            .map { (muscle, v) ->
                TopMuscleContribution(
                    muscle = muscle,
                    share = if (totalMuscle > EPS) {
                        ((v / totalMuscle) * 100f).roundToInt().coerceIn(0, 100)
                    } else 0,
                )
            }

        val topExercises = buildTopExercises(allStats, totalStimulus)

        return Analysis(
            raw = pool,
            sessionCount = sessionCount,
            strengthShare = strengthShare,
            hypertrophyShare = hypertrophyShare,
            enduranceShare = enduranceShare,
            compoundRatio = compoundRatio,
            topExercises = topExercises,
            topMuscles = topMuscles,
        )
    }

    private fun emptyAnalysis(): Analysis = Analysis(
        raw = Raw(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        sessionCount = 0,
        strengthShare = 0,
        hypertrophyShare = 0,
        enduranceShare = 0,
        compoundRatio = 0,
        topExercises = emptyList(),
        topMuscles = emptyList(),
    )

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
    )

    private fun buildTopExercises(
        allStats: List<ExerciseStats>,
        totalStimulus: Float,
    ): List<TopExerciseContribution> {
        if (allStats.isEmpty()) return emptyList()
        val grouped: Map<String, List<ExerciseStats>> = allStats.groupBy { it.exampleId }
        return grouped.map { (exampleId, list) ->
            val stimulusSum = list.sumOf { it.stimulus.toDouble() }.toFloat()
            val setsCount = list.sumOf { it.sets.size }
            val heaviest = list
                .flatMap { it.sets }
                .maxOfOrNull { it.weight } ?: 0f
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
    // Per-goal adherence formulas — now just return Int score.
    // -------------------------------------------------------------------------

    private fun scoreForPrimary(primary: GoalPrimaryGoalEnum, analysis: Analysis): Int {
        return when (primary) {
            GoalPrimaryGoalEnum.GET_STRONGER -> scoreStrengthFocus(analysis)
            GoalPrimaryGoalEnum.BUILD_MUSCLE -> scoreHypertrophyFocus(analysis)
            GoalPrimaryGoalEnum.LOSE_FAT -> scoreEnduranceFocus(analysis)
            GoalPrimaryGoalEnum.MAINTAIN -> scorePowerbuilding(analysis)
            GoalPrimaryGoalEnum.GENERAL_FITNESS -> scoreConcurrent(analysis)
            GoalPrimaryGoalEnum.RETURN_TO_TRAINING -> scoreReturnToTraining(analysis)
        }
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
        val overdoPenalty =
            if (overdoRange <= EPS) 1f
            else (1f - (work - RTT_WORK_PEAK) / overdoRange).coerceIn(0f, 1f)
        return toScore(activityFloor * overdoPenalty)
    }

    private fun scoreConsistency(a: Analysis): Int {
        val target = CONSISTENCY_TARGET_SESSIONS.coerceAtLeast(1)
        val ratio = (a.sessionCount.toFloat() / target).coerceIn(0f, 1f)
        return toScore(ratio)
    }

    /**
     * Smooth penalty applied to the third (suppressed) axis of a balanced-dual
     * goal. Returns 1 when the suppressed axis is ~0, and 0 when it reaches
     * [SUPPRESSED_AXIS_PENALTY_FULL] share (50%).
     */
    private fun suppressedAxisPenalty(suppressedShare: Float): Float {
        val t = (suppressedShare / SUPPRESSED_AXIS_PENALTY_FULL).coerceIn(0f, 1f)
        return 1f - t
    }

    private fun toScore(x: Float): Int {
        val v = if (x.isFinite()) x else 0f
        return (v * 100f).roundToInt().coerceIn(0, 100)
    }

    // -------------------------------------------------------------------------
    // Per-session Raw + pooled aggregation.
    // -------------------------------------------------------------------------

    private suspend fun loadExamples(ids: Set<String>): Map<String, ExerciseExample> {
        if (ids.isEmpty()) return emptyMap()
        return exerciseExampleFeature.observeExerciseExamples(ids.toList())
            .first()
            .associateBy { it.value.id }
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
    )

    private data class LiftSet(
        val weight: Float,
        val reps: Int
    )

    private data class SetStats(
        val weight: Float,
        val reps: Int,
        val pct: Float,
        val stimulus: Float,
    )

    private data class ExerciseStats(
        val exercise: Exercise,
        val exampleId: String,
        val sets: List<SetStats>,
        val stimulus: Float,
    )

    private data class DimensionWork(
        val strength: Float,
        val hypertrophy: Float,
        val endurance: Float,
    )

    private fun buildRawFromStats(
        training: Training,
        stats: List<ExerciseStats>,
        exampleMap: Map<String, ExerciseExample>,
    ): Raw? {
        if (stats.isEmpty()) return null

        val totalSets = stats.sumOf { it.sets.size }
        val safeMinutes =
            resolveMinutes(training.duration.inWholeSeconds.toFloat() / 60f, totalSets)

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
        val stimulusPerMin = if (stimulus > EPS) stimulus / safeMinutes else 0f

        val reps = training.repetitions.coerceAtLeast(0)
        val repsPerMin = if (reps > 0) reps.toFloat() / safeMinutes else 0f

        val compoundRatio = weightedCategoryRatioByStimulus(stats, CategoryEnum.COMPOUND)
        val freeWeightRatio = weightedWeightTypeRatioByStimulus(stats, WeightTypeEnum.FREE)
        val specializationTop2Percent = computeSpecializationTop2Percent(stats, exampleMap)

        val activity = safe(
            log1pSafe(stimulusPerMin) +
                    log1pSafe(repsPerMin) +
                    log1pSafe(totalDimWork / safeMinutes)
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

    private fun resolveMinutes(minutes: Float, totalSets: Int): Float {
        if (minutes.isFinite() && minutes >= 5f) return minutes
        val sets = totalSets.coerceAtLeast(1)
        val estimated = (sets.toFloat() * 1.4f).coerceIn(10f, 180f)
        return if (estimated.isFinite() && estimated > 0f) estimated else 10f
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
                SetStats(
                    weight = set.weight,
                    reps = set.reps,
                    pct = pct,
                    stimulus = base,
                )
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
        if (raws.isEmpty()) return Raw(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

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
            val w = weightFor(r)
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

        val denom = if (sumW > EPS) sumW else 1f

        return Raw(
            strengthShare = clamp01(strength / denom),
            hypertrophyShare = clamp01(hypertrophy / denom),
            enduranceShare = clamp01(endurance / denom),
            compoundRatio = clamp01(compound / denom),
            freeWeightRatio = clamp01(free / denom),
            specializationTop2Percent = (spec / denom).coerceIn(0f, 100f),
            activity = safe(activity / denom),
            totalWork = safe(totalWork / denom),
        )
    }

    private fun weightFor(raw: Raw): Float {
        val w = 0.25f + raw.activity
        return if (w.isFinite() && w > EPS) w else 1f
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
        } else {
            when {
                pct < 0.50f -> 0.20f
                pct < 0.60f -> 0.70f
                pct < 0.76f -> 1.00f
                pct < 0.86f -> 0.65f
                else -> 0.25f
            }
        }
        return (repFactor * pctFactor).coerceIn(0f, 1f)
    }

    private fun enduranceMembership(reps: Int, pct: Float): Float {
        val r = reps.coerceAtLeast(0)
        val repFactor = when {
            r <= 8 -> 0.05f
            r in 9..12 -> 0.25f
            r in 13..15 -> 0.75f
            r in 16..25 -> 1.00f
            else -> 1.00f
        }
        val pctFactor = if (!pct.isFinite()) {
            if (r >= 12) 1.0f else 0.6f
        } else {
            when {
                pct <= 0.55f -> 1.00f
                pct <= 0.65f -> 0.75f
                pct <= 0.75f -> 0.45f
                else -> 0.20f
            }
        }
        return (repFactor * pctFactor).coerceIn(0f, 1f)
    }

    private fun Iteration.toLiftSetOrNull(): LiftSet? {
        val weight = this.volume
        val reps = this.repetitions
        if (!weight.isFinite() || weight <= EPS) return null
        if (reps <= 0) return null
        return LiftSet(weight = weight, reps = reps)
    }

    private fun estimateExerciseOneRm(sets: List<LiftSet>): Float {
        val working = pickWorkingSets(sets, WORKING_MIN_WEIGHT_FRACTION, WORKING_MIN_SETS)
        val e1rms = working
            .map { epleyE1rm(it.weight, it.reps) }
            .filter { it.isFinite() && it > EPS }
            .sorted()
        if (e1rms.isEmpty()) return 0f
        val p = percentileSorted(e1rms, HEAVY_E1RM_PERCENTILE)
        return if (p.isFinite() && p > EPS) p else 0f
    }

    private fun pickWorkingSets(
        sets: List<LiftSet>,
        minFraction: Float,
        minSets: Int,
    ): List<LiftSet> {
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

        val thr = ref * minFraction
        val picked = sets.filter { it.weight.isFinite() && it.weight >= thr }
        return if (picked.size >= minSets) picked else sets
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

    private fun epleyE1rm(weight: Float, reps: Int): Float {
        val r = reps.coerceIn(1, ONE_RM_REPS_CAP)
        val mult = 1f + ((r - 1).toFloat() / 30f)
        val v = weight * mult
        return if (v.isFinite() && v > 0f) v else 0f
    }

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

                val muscle = bundle.muscle.type
                val add = base * ratio
                if (!add.isFinite() || add <= EPS) return@forEach

                totals[muscle] = (totals[muscle] ?: 0f) + add
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

    private fun clamp01(v: Float): Float {
        return if (v.isFinite()) v.coerceIn(0f, 1f) else 0f
    }

    private fun safe(v: Float): Float {
        return if (v.isFinite() && v > 0f) v else 0f
    }

    private fun log1pSafe(v: Float): Float {
        if (!v.isFinite() || v <= 0f) return 0f
        return ln(1.0 + v.toDouble()).toFloat()
    }
}
