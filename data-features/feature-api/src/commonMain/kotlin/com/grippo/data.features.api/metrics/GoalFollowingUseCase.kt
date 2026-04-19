package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.GoalPrimaryGoalEnum
import com.grippo.data.features.api.goal.models.GoalSecondaryGoalEnum
import com.grippo.data.features.api.metrics.GoalFollowingUseCase.Companion.SUPPRESSED_AXIS_PENALTY_FULL
import com.grippo.data.features.api.metrics.models.GoalAdherence
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
         * BUILD_MUSCLE / LOSE_FAT). The core axis share provides [CORE_WEIGHT]
         * of the score; the remainder is split among quality modifiers. The
         * three weights below always sum to 1 so a perfect-on-axis + perfect-on-
         * quality session scores exactly 100.
         */
        private const val SPEC_CORE_WEIGHT = 0.70f
        private const val SPEC_QUALITY_WEIGHT = 0.30f

        /** Saturation point of the activity-density quality signal. */
        private const val ACTIVITY_QUALITY_SATURATION = 3f

        /** Saturation point of the top-2 muscle specialization signal. */
        private const val SPECIALIZATION_SATURATION_PERCENT = 80f

        /**
         * Balanced-dual goals (MAINTAIN, GENERAL_FITNESS) score as
         * `min(axisA, axisB) * 2`. That peaks at 100 when both axes are at 50%
         * and the third axis is 0. A penalty clamps the score when the
         * suppressed axis grows beyond the threshold below.
         */
        private const val SUPPRESSED_AXIS_PENALTY_FULL = 0.50f

        /**
         * RETURN_TO_TRAINING scoring: adherence peaks in a "low-to-moderate"
         * work band and decays outside it. Athletes returning must train *some*
         * but not *too much*; both extremes lower adherence.
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
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public suspend fun fromTrainingsByPrimary(
        trainings: List<Training>,
    ): GoalAdherence? {
        val goal = goalFeature.observeGoal().firstOrNull() ?: return null
        val pooled = computePooled(trainings)
        return scoreForPrimary(goal, goal.primaryGoal, pooled)
    }

    public suspend fun fromTrainingsBySecondary(
        trainings: List<Training>,
    ): GoalAdherence? {
        val goal = goalFeature.observeGoal().firstOrNull() ?: return null
        val pooled = computePooled(trainings)
        return when (goal.secondaryGoal) {
            GoalSecondaryGoalEnum.GET_STRONGER ->
                scoreForPrimary(goal, GoalPrimaryGoalEnum.GET_STRONGER, pooled)

            GoalSecondaryGoalEnum.BUILD_MUSCLE ->
                scoreForPrimary(goal, GoalPrimaryGoalEnum.BUILD_MUSCLE, pooled)

            GoalSecondaryGoalEnum.LOSE_FAT ->
                scoreForPrimary(goal, GoalPrimaryGoalEnum.LOSE_FAT, pooled)

            GoalSecondaryGoalEnum.MAINTAIN ->
                scoreForPrimary(goal, GoalPrimaryGoalEnum.MAINTAIN, pooled)

            GoalSecondaryGoalEnum.GENERAL_FITNESS ->
                scoreForPrimary(goal, GoalPrimaryGoalEnum.GENERAL_FITNESS, pooled)

            GoalSecondaryGoalEnum.RETURN_TO_TRAINING ->
                scoreForPrimary(goal, GoalPrimaryGoalEnum.RETURN_TO_TRAINING, pooled)

            GoalSecondaryGoalEnum.CONSISTENCY ->
                scoreConsistency(goal, pooled)

            null -> null
        }
    }

    // -------------------------------------------------------------------------
    // Per-goal adherence formulas
    // -------------------------------------------------------------------------

    private fun scoreForPrimary(
        goal: Goal,
        primary: GoalPrimaryGoalEnum,
        pooled: Pooled,
    ): GoalAdherence {
        val strength = pooled.strength
        val hypertrophy = pooled.hypertrophy
        val endurance = pooled.endurance

        val score = when (primary) {
            GoalPrimaryGoalEnum.GET_STRONGER -> scoreStrengthFocus(pooled)
            GoalPrimaryGoalEnum.BUILD_MUSCLE -> scoreHypertrophyFocus(pooled)
            GoalPrimaryGoalEnum.LOSE_FAT -> scoreEnduranceFocus(pooled)
            GoalPrimaryGoalEnum.MAINTAIN -> scorePowerbuilding(pooled)
            GoalPrimaryGoalEnum.GENERAL_FITNESS -> scoreConcurrent(pooled)
            GoalPrimaryGoalEnum.RETURN_TO_TRAINING -> scoreReturnToTraining(pooled)
        }

        return GoalAdherence(
            goal = goal,
            score = score,
            strengthShare = strength,
            hypertrophyShare = hypertrophy,
            enduranceShare = endurance,
        )
    }

    private fun scoreStrengthFocus(pooled: Pooled): Int {
        if (pooled.isSilent) return 0
        // Core = strength share. Quality = compound + free-weight signal.
        val core = pooled.raw.strengthShare
        val quality = 0.5f * pooled.raw.compoundRatio + 0.5f * pooled.raw.freeWeightRatio
        return toScore(core * (SPEC_CORE_WEIGHT + SPEC_QUALITY_WEIGHT * quality))
    }

    private fun scoreHypertrophyFocus(pooled: Pooled): Int {
        if (pooled.isSilent) return 0
        // Core = hypertrophy share. Quality = muscle specialization (focus).
        val core = pooled.raw.hypertrophyShare
        val quality = (pooled.raw.specializationTop2Percent / SPECIALIZATION_SATURATION_PERCENT)
            .coerceIn(0f, 1f)
        return toScore(core * (SPEC_CORE_WEIGHT + SPEC_QUALITY_WEIGHT * quality))
    }

    private fun scoreEnduranceFocus(pooled: Pooled): Int {
        if (pooled.isSilent) return 0
        // Core = endurance share. Quality = density (activity index).
        val core = pooled.raw.enduranceShare
        val quality = (pooled.raw.activity / ACTIVITY_QUALITY_SATURATION).coerceIn(0f, 1f)
        return toScore(core * (SPEC_CORE_WEIGHT + SPEC_QUALITY_WEIGHT * quality))
    }

    private fun scorePowerbuilding(pooled: Pooled): Int {
        if (pooled.isSilent) return 0
        // Balance = min(str, hyp) * 2 peaks at 50/50 split; penalty scales with
        // how much endurance intruded on the session.
        val balance = 2f * min(pooled.raw.strengthShare, pooled.raw.hypertrophyShare)
        val penalty = suppressedAxisPenalty(pooled.raw.enduranceShare)
        return toScore(balance * penalty)
    }

    private fun scoreConcurrent(pooled: Pooled): Int {
        if (pooled.isSilent) return 0
        // Same shape as powerbuilding but str+end with hypertrophy suppressed.
        val balance = 2f * min(pooled.raw.strengthShare, pooled.raw.enduranceShare)
        val penalty = suppressedAxisPenalty(pooled.raw.hypertrophyShare)
        return toScore(balance * penalty)
    }

    private fun scoreReturnToTraining(pooled: Pooled): Int {
        // A ramp-up phase needs *some* work (floor) and *not too much* (ceiling).
        val work = pooled.raw.totalWork
        val activityFloor = (work / RTT_WORK_FLOOR).coerceIn(0f, 1f)
        val overdoRange = RTT_WORK_CEILING - RTT_WORK_PEAK
        val overdoPenalty =
            if (overdoRange <= EPS) 1f
            else (1f - (work - RTT_WORK_PEAK) / overdoRange).coerceIn(0f, 1f)
        return toScore(activityFloor * overdoPenalty)
    }

    private fun scoreConsistency(goal: Goal, pooled: Pooled): GoalAdherence {
        // CONSISTENCY is frequency-based, not stimulus-based.
        val target = CONSISTENCY_TARGET_SESSIONS.coerceAtLeast(1)
        val ratio = (pooled.sessionCount.toFloat() / target).coerceIn(0f, 1f)
        return GoalAdherence(
            goal = goal,
            score = toScore(ratio),
            strengthShare = pooled.strength,
            hypertrophyShare = pooled.hypertrophy,
            enduranceShare = pooled.endurance,
        )
    }

    /**
     * Smooth penalty applied to the third (suppressed) axis of a balanced-dual
     * goal. Returns 1 when the suppressed axis is ~0, and 0 when it reaches
     * [SUPPRESSED_AXIS_PENALTY_FULL] share (50%) — i.e. as soon as the third
     * axis is as big as either of the balanced pair the score collapses.
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
    // Pooled primitives (self-contained; mirrors TrainingLoadProfileUseCase)
    // -------------------------------------------------------------------------

    private data class Pooled(
        val raw: Raw,
        val sessionCount: Int,
        val strength: Int,
        val hypertrophy: Int,
        val endurance: Int,
    ) {
        val isSilent: Boolean
            get() = raw.totalWork <= EPS || (strength + hypertrophy + endurance) == 0
    }

    private suspend fun computePooled(trainings: List<Training>): Pooled {
        if (trainings.isEmpty()) return emptyPooled()

        val ids = trainings.flatMap { it.exercises }.map { it.exerciseExample.id }.toSet()
        val examples = loadExamples(ids)

        val raws = trainings.mapNotNull { buildRaw(it, examples) }
        if (raws.isEmpty()) return emptyPooled()

        val pool = aggregateRaw(raws)

        val strength = (pool.strengthShare * 100f).roundToInt().coerceIn(0, 100)
        val hypertrophy = (pool.hypertrophyShare * 100f).roundToInt().coerceIn(0, 100)
        val endurance = (pool.enduranceShare * 100f).roundToInt().coerceIn(0, 100)

        return Pooled(
            raw = pool,
            sessionCount = raws.size,
            strength = strength,
            hypertrophy = hypertrophy,
            endurance = endurance,
        )
    }

    private fun emptyPooled(): Pooled = Pooled(
        raw = Raw(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        sessionCount = 0,
        strength = 0,
        hypertrophy = 0,
        endurance = 0,
    )

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

    private fun buildRaw(training: Training, exampleMap: Map<String, ExerciseExample>): Raw? {
        return buildRaw(
            exercises = training.exercises,
            minutes = training.duration.inWholeSeconds.toFloat() / 60f,
            repetitions = training.repetitions,
            exampleMap = exampleMap,
        )
    }

    private fun buildRaw(
        exercises: List<Exercise>,
        minutes: Float,
        repetitions: Int,
        exampleMap: Map<String, ExerciseExample>,
    ): Raw? {
        if (exercises.isEmpty()) return null

        val stats = buildExerciseStats(exercises)
        if (stats.isEmpty()) return null

        val totalSets = stats.sumOf { it.sets.size }
        val safeMinutes = resolveMinutes(minutes, totalSets)

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

        val reps = repetitions.coerceAtLeast(0)
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

        val values = totals.values.filter { it.isFinite() && it > EPS }.sortedDescending()
        if (values.isEmpty()) return 0f

        val sum = values.sum()
        if (!sum.isFinite() || sum <= EPS) return 0f

        val top2 = values.take(2).sum()
        val percent = (top2 / sum) * 100f
        return if (percent.isFinite()) percent.coerceIn(0f, 100f) else 0f
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
