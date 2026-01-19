package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.metrics.models.TrainingDimensionKind
import com.grippo.data.features.api.metrics.models.TrainingDimensionScore
import com.grippo.data.features.api.metrics.models.TrainingLoadProfile
import com.grippo.data.features.api.metrics.models.TrainingProfileKind
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

public class TrainingLoadProfileUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
) {

    private companion object {
        private const val EPS = 1e-3f

        private const val STIMULUS_LN_WEIGHT_SCALE = 8f

        private const val WORKING_MIN_WEIGHT_FRACTION = 0.75f
        private const val WORKING_MIN_SETS = 2
        private const val WORKING_REFERENCE_TOP_SETS = 3

        private const val ONE_RM_REPS_CAP = 12
        private const val HEAVY_E1RM_PERCENTILE = 0.90f

        private const val EASY_TOTAL_WORK_EPS = 30f

        private const val CONFIDENCE_GAP_MAX = 25f
        private const val CONFIDENCE_MIN_TOP_SCORE = 40
    }

    public suspend fun fromTrainings(trainings: List<Training>): TrainingLoadProfile {
        val ids = trainings.flatMap { it.exercises }.map { it.exerciseExample.id }.toSet()
        val examples = loadExamples(ids)

        val raws = trainings.mapNotNull { buildRaw(it, examples) }
        if (raws.isEmpty()) return emptyProfile()

        return buildProfile(raws)
    }

    public suspend fun fromTraining(training: Training): TrainingLoadProfile {
        return fromTrainings(listOf(training))
    }

    public suspend fun fromExercises(exercises: List<Exercise>): TrainingLoadProfile {
        if (exercises.isEmpty()) return emptyProfile()

        val examples = loadExamples(exercises.map { it.exerciseExample.id }.toSet())
        val raw = buildRaw(
            exercises = exercises,
            minutes = 0f,
            repetitions = exercises.sumOf { it.repetitions },
            exampleMap = examples,
        ) ?: return emptyProfile()

        return buildProfile(listOf(raw))
    }

    private suspend fun loadExamples(ids: Set<String>): Map<String, ExerciseExample> {
        if (ids.isEmpty()) return emptyMap()
        return exerciseExampleFeature.observeExerciseExamples(ids.toList())
            .first()
            .associateBy { it.value.id }
    }

    private fun emptyProfile(): TrainingLoadProfile {
        return TrainingLoadProfile(
            kind = TrainingProfileKind.Easy,
            dimensions = listOf(
                TrainingDimensionScore(TrainingDimensionKind.Strength, 0),
                TrainingDimensionScore(TrainingDimensionKind.Hypertrophy, 0),
                TrainingDimensionScore(TrainingDimensionKind.Endurance, 0),
            ),
            dominant = null,
            confidence = 0,
        )
    }

    private data class Raw(
        val strengthShare: Float,
        val hypertrophyShare: Float,
        val enduranceShare: Float,
        val compoundRatio: Float,
        val freeWeightRatio: Float,
        val pushRatio: Float,
        val specializationTop2Percent: Float,
        val activity: Float,
        val totalWork: Float,
    )

    private fun buildRaw(training: Training, exampleMap: Map<String, ExerciseExample>): Raw? {
        return buildRaw(
            exercises = training.exercises,
            minutes = training.duration.inWholeSeconds.toFloat() / 60f,
            repetitions = training.repetitions,
            exampleMap = exampleMap,
        )
    }

    private data class LiftSet(
        val weight: Float,
        val reps: Int,
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
        val pushRatio = weightedPushRatioByStimulus(stats)

        val specializationTop2Percent = computeSpecializationTop2Percent(stats, exampleMap)

        val activity = safe(
            log1pSafe(stimulusPerMin) + log1pSafe(repsPerMin) + log1pSafe(totalDimWork / safeMinutes)
        )

        return Raw(
            strengthShare = strengthShare,
            hypertrophyShare = hypertrophyShare,
            enduranceShare = enduranceShare,
            compoundRatio = clamp01(compoundRatio),
            freeWeightRatio = clamp01(freeWeightRatio),
            pushRatio = clamp01(pushRatio),
            specializationTop2Percent = specializationTop2Percent.coerceIn(0f, 100f),
            activity = activity,
            totalWork = safe(totalDimWork),
        )
    }

    private fun resolveMinutes(minutes: Float, totalSets: Int): Float {
        val m = minutes
        if (m.isFinite() && m >= 5f) return m

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

    private fun buildProfile(raws: List<Raw>): TrainingLoadProfile {
        val poolRaw = aggregateRaw(raws)

        val strength = (poolRaw.strengthShare * 100f).roundToInt().coerceIn(0, 100)
        val hypertrophy = (poolRaw.hypertrophyShare * 100f).roundToInt().coerceIn(0, 100)
        val endurance = (poolRaw.enduranceShare * 100f).roundToInt().coerceIn(0, 100)

        val dominant = dominantAxis(strength, hypertrophy, endurance)
        val confidence = confidenceScore(strength, hypertrophy, endurance)

        val kind = classify(
            raw = poolRaw,
            strength = strength,
            hypertrophy = hypertrophy,
            endurance = endurance,
            dominant = dominant,
            confidence = confidence,
        )

        return TrainingLoadProfile(
            kind = kind,
            dimensions = listOf(
                TrainingDimensionScore(TrainingDimensionKind.Strength, strength),
                TrainingDimensionScore(TrainingDimensionKind.Hypertrophy, hypertrophy),
                TrainingDimensionScore(TrainingDimensionKind.Endurance, endurance),
            ),
            dominant = if (kind == TrainingProfileKind.Easy) null else dominant,
            confidence = if (kind == TrainingProfileKind.Easy) 0 else confidence,
        )
    }

    private fun classify(
        raw: Raw,
        strength: Int,
        hypertrophy: Int,
        endurance: Int,
        dominant: TrainingDimensionKind?,
        confidence: Int,
    ): TrainingProfileKind {
        if (raw.totalWork <= EASY_TOTAL_WORK_EPS) return TrainingProfileKind.Easy

        val isPowerbuilding =
            endurance <= 20 &&
                    strength >= 35 &&
                    hypertrophy >= 35 &&
                    abs(strength - hypertrophy) <= 10

        if (isPowerbuilding) return TrainingProfileKind.Powerbuilding

        val isConcurrent =
            hypertrophy <= 25 &&
                    strength >= 35 &&
                    endurance >= 35 &&
                    abs(strength - endurance) <= 12

        if (isConcurrent) return TrainingProfileKind.Mixed

        if (dominant != null && confidence >= 60) {
            return when (dominant) {
                TrainingDimensionKind.Strength -> TrainingProfileKind.Strength
                TrainingDimensionKind.Hypertrophy -> TrainingProfileKind.Hypertrophy
                TrainingDimensionKind.Endurance -> TrainingProfileKind.Endurance
            }
        }

        return TrainingProfileKind.Mixed
    }

    private fun aggregateRaw(raws: List<Raw>): Raw {
        if (raws.isEmpty()) {
            return Raw(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        }

        var sumW = 0f
        var strength = 0f
        var hypertrophy = 0f
        var endurance = 0f
        var compound = 0f
        var free = 0f
        var push = 0f
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
            push += w * r.pushRatio
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
            pushRatio = clamp01(push / denom),
            specializationTop2Percent = (spec / denom).coerceIn(0f, 100f),
            activity = safe(activity / denom),
            totalWork = safe(totalWork / denom),
        )
    }

    private fun weightFor(raw: Raw): Float {
        val w = 0.25f + raw.activity
        return if (w.isFinite() && w > EPS) w else 1f
    }

    private data class DimensionWork(
        val strength: Float,
        val hypertrophy: Float,
        val endurance: Float,
    )

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
        val working = pickWorkingSets(
            sets = sets,
            minFraction = WORKING_MIN_WEIGHT_FRACTION,
            minSets = WORKING_MIN_SETS,
        )

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

        val weights = sets
            .asSequence()
            .map { it.weight }
            .filter { it.isFinite() && it > EPS }
            .toList()

        if (weights.isEmpty()) return emptyList()

        val topCount = min(WORKING_REFERENCE_TOP_SETS, weights.size)
        val topAsc = weights
            .sortedDescending()
            .take(topCount)
            .sorted()

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

    private fun setStimulus(weight: Float, reps: Int): Float {
        val r = reps.coerceAtLeast(0)
        if (r == 0) return 0f

        val w = weight.coerceAtLeast(0f)

        val repsTerm = sqrt(r.toFloat())
        val weightTerm = ln(1.0 + w.toDouble()).toFloat()
        val intensity = 1f + (weightTerm / (STIMULUS_LN_WEIGHT_SCALE * 0.55f))

        val v = repsTerm * intensity
        return if (v.isFinite() && v > EPS) v else 0f
    }

    private fun setStimulus(weight: Float, reps: Int, pct: Float): Float {
        val base = setStimulus(weight, reps)
        if (base <= EPS) return 0f
        if (!pct.isFinite()) return base

        val p = pct.coerceIn(0f, 1.2f)
        val boost = 0.75f + 0.50f * smoothStep((p - 0.65f) / 0.25f)

        val v = base * boost
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
        category: CategoryEnum
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
        type: WeightTypeEnum
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

    private fun weightedPushRatioByStimulus(stats: List<ExerciseStats>): Float {
        var sum = 0f
        var push = 0f

        stats.forEach { ex ->
            val w = ex.stimulus
            if (!w.isFinite() || w <= EPS) return@forEach

            sum += w
            if (isPushForceType(ex.exercise.exerciseExample.forceType)) {
                push += w
            }
        }

        if (!sum.isFinite() || sum <= EPS) return 0f
        val r = push / sum
        return if (r.isFinite()) r.coerceIn(0f, 1f) else 0f
    }

    private fun isPushForceType(forceType: ForceTypeEnum?): Boolean {
        return forceType == ForceTypeEnum.PUSH
    }

    private fun dominantAxis(s: Int, h: Int, e: Int): TrainingDimensionKind? {
        val top = max(s, max(h, e))
        val isStrength = s == top
        val isHypertrophy = h == top
        val isEndurance = e == top

        return when {
            isStrength && !isHypertrophy && !isEndurance -> TrainingDimensionKind.Strength
            isHypertrophy && !isStrength && !isEndurance -> TrainingDimensionKind.Hypertrophy
            isEndurance && !isStrength && !isHypertrophy -> TrainingDimensionKind.Endurance
            else -> null
        }
    }

    private fun confidenceScore(s: Int, h: Int, e: Int): Int {
        val top = max(s, max(h, e))
        val second = secondOfThree(s, h, e)
        val gap = (top - second).toFloat().coerceAtLeast(0f)

        if (top < CONFIDENCE_MIN_TOP_SCORE) return 0

        val gapScore = (gap / CONFIDENCE_GAP_MAX).coerceIn(0f, 1f)
        val topScore = ((top - 35).toFloat() / 40f).coerceIn(0f, 1f)

        val combined = gapScore * (0.5f + 0.5f * topScore)
        return (combined * 100f).roundToInt().coerceIn(0, 100)
    }

    private fun secondOfThree(a: Int, b: Int, c: Int): Int {
        return a + b + c - max(a, max(b, c)) - min(a, min(b, c))
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
}

private fun log1pSafe(v: Float): Float {
    if (!v.isFinite() || v <= 0f) return 0f
    return ln(1.0 + v.toDouble()).toFloat()
}
