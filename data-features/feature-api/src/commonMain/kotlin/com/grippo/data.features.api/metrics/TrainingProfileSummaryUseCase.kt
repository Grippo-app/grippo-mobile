package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
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

public class TrainingLoadProfileUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
) {

    private companion object {
        private const val EPS = 1e-3f

        private const val STIMULUS_LN_WEIGHT_SCALE = 8f

        private const val STRENGTH_TOP_EXERCISES = 3

        private const val WORKING_MIN_WEIGHT_FRACTION = 0.75f
        private const val WORKING_MIN_SETS = 2
        private const val WORKING_REFERENCE_TOP_SETS = 3

        private const val ONE_RM_REPS_CAP = 12
        private const val HEAVY_E1RM_PERCENTILE = 0.80f

        private const val STRENGTH_COMPOUND_BONUS = 1.10f
        private const val STRENGTH_ISOLATION_FACTOR = 0.96f

        private const val STRENGTH_FREE_WEIGHT_BONUS = 1.05f
        private const val STRENGTH_FIXED_WEIGHT_FACTOR = 1.00f
        private const val STRENGTH_BODY_WEIGHT_FACTOR = 1.02f

        private const val ENDURANCE_STIMULUS_SHARE = 0.70f
        private const val ENDURANCE_REPS_SHARE = 0.30f

        private const val EASY_ACTIVITY_EPS = 0.02f

        private const val KIND_RANK_TOP_MIN = 0.60f
        private const val KIND_RANK_GAP_MIN = 0.12f

        private const val POWERBUILDING_RANK_MIN = 0.70f
        private const val POWERBUILDING_RANK_GAP_MAX = 0.12f
        private const val POWERBUILDING_COMPOUND_RANK_MIN = 0.50f
    }

    public suspend fun fromTrainings(trainings: List<Training>): TrainingLoadProfile {
        val examples =
            loadExamples(trainings.flatMap { it.exercises }.map { it.exerciseExample.id }.toSet())
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
        val repetitions = exercises.sumOf { it.repetitions }
        val raw = buildRaw(
            exercises = exercises,
            minutes = 0f,
            repetitions = repetitions,
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
        )
    }

    private data class Raw(
        val strengthRaw: Float,
        val hypertrophyRaw: Float,
        val enduranceRaw: Float,
        val compoundRatio: Float,
        val freeWeightRatio: Float,
        val pushRatio: Float,
        val specializationTop2Percent: Float,
        val activity: Float,
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

        val safeMinutes = if (minutes.isFinite() && minutes > 0.5f) minutes else 1f

        val stimulus = computeStimulus(exercises)
        val stimulusPerMin =
            if (stimulus.isFinite() && stimulus > EPS) stimulus / safeMinutes else 0f

        val reps = repetitions.coerceAtLeast(0)
        val repsPerMin = if (reps > 0) reps.toFloat() / safeMinutes else 0f

        val compoundRatio = weightedCategoryRatioByStimulus(exercises, CategoryEnum.COMPOUND)
        val freeWeightRatio = weightedWeightTypeRatioByStimulus(exercises, WeightTypeEnum.FREE)
        val pushRatio = weightedPushRatioByStimulus(exercises)

        val strengthRaw = strengthFromTopExercises(exercises)
        val enduranceRaw = repsPerMin

        val specializationTop2Percent = computeSpecializationTop2Percent(exercises, exampleMap)

        val activity = safe(
            log1pSafe(strengthRaw) + log1pSafe(stimulusPerMin) + log1pSafe(repsPerMin)
        )

        return Raw(
            strengthRaw = safe(strengthRaw),
            hypertrophyRaw = safe(stimulus),
            enduranceRaw = safe(enduranceRaw),
            compoundRatio = clamp01(compoundRatio),
            freeWeightRatio = clamp01(freeWeightRatio),
            pushRatio = clamp01(pushRatio),
            specializationTop2Percent = specializationTop2Percent.coerceIn(0f, 100f),
            activity = activity,
        )
    }

    private fun buildProfile(raws: List<Raw>): TrainingLoadProfile {
        val normalizer = buildNormalizer(raws)
        val ranks = buildRanksByRaw(raws)

        val poolRaw = aggregateRaw(raws)
        val poolScore = computeScores(poolRaw, normalizer)
        val poolRank = ranks.rankFor(poolRaw)

        val kind = classifyWithinPool(
            raw = poolRaw,
            score = poolScore,
            rank = poolRank,
        )

        return TrainingLoadProfile(
            kind = kind,
            dimensions = listOf(
                TrainingDimensionScore(TrainingDimensionKind.Strength, poolScore.strength),
                TrainingDimensionScore(TrainingDimensionKind.Hypertrophy, poolScore.hypertrophy),
                TrainingDimensionScore(TrainingDimensionKind.Endurance, poolScore.endurance),
            ),
        )
    }

    private fun aggregateRaw(raws: List<Raw>): Raw {
        if (raws.isEmpty()) {
            return Raw(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
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

        raws.forEach { r ->
            val w = weightFor(r)
            sumW += w

            strength += w * r.strengthRaw
            hypertrophy += w * r.hypertrophyRaw
            endurance += w * r.enduranceRaw
            compound += w * r.compoundRatio
            free += w * r.freeWeightRatio
            push += w * r.pushRatio
            spec += w * r.specializationTop2Percent
            activity += w * r.activity
        }

        val denom = if (sumW.isFinite() && sumW > EPS) sumW else 1f

        return Raw(
            strengthRaw = safe(strength / denom),
            hypertrophyRaw = safe(hypertrophy / denom),
            enduranceRaw = safe(endurance / denom),
            compoundRatio = clamp01(compound / denom),
            freeWeightRatio = clamp01(free / denom),
            pushRatio = clamp01(push / denom),
            specializationTop2Percent = (spec / denom).coerceIn(0f, 100f),
            activity = safe(activity / denom),
        )
    }

    private fun weightFor(raw: Raw): Float {
        val w = 0.25f + raw.activity
        return if (w.isFinite() && w > EPS) w else 1f
    }

    private data class StrengthItem(
        val pickScore: Float,
        val heavyE1rm: Float,
    )

    private fun strengthFromTopExercises(exercises: List<Exercise>): Float {
        val items = exercises.mapNotNull { e ->
            val heavy = heavyE1rmSignalForIterations(e.iterations)
            if (!heavy.isFinite() || heavy <= EPS) return@mapNotNull null

            val categoryFactor = when (e.exerciseExample.category) {
                CategoryEnum.COMPOUND -> STRENGTH_COMPOUND_BONUS
                CategoryEnum.ISOLATION -> STRENGTH_ISOLATION_FACTOR
            }

            val weightTypeFactor = when (e.exerciseExample.weightType) {
                WeightTypeEnum.FREE -> STRENGTH_FREE_WEIGHT_BONUS
                WeightTypeEnum.FIXED -> STRENGTH_FIXED_WEIGHT_FACTOR
                WeightTypeEnum.BODY_WEIGHT -> STRENGTH_BODY_WEIGHT_FACTOR
            }

            val factor = categoryFactor * weightTypeFactor
            if (!factor.isFinite() || factor <= 0f) return@mapNotNull null

            val pickScore = heavy * factor
            if (!pickScore.isFinite() || pickScore <= EPS) return@mapNotNull null

            StrengthItem(
                pickScore = pickScore,
                heavyE1rm = heavy,
            )
        }

        if (items.isEmpty()) return 0f

        val top = items
            .sortedByDescending { it.pickScore }
            .take(STRENGTH_TOP_EXERCISES)

        val sumW = top.sumOf { it.pickScore.toDouble() }.toFloat()
        if (!sumW.isFinite() || sumW <= EPS) return 0f

        val weighted = top.sumOf { (it.pickScore * it.heavyE1rm).toDouble() }.toFloat()
        if (!weighted.isFinite() || weighted <= EPS) return 0f

        val v = weighted / sumW
        return if (v.isFinite() && v > EPS) v else 0f
    }

    private fun heavyE1rmSignalForIterations(iterations: List<Iteration>): Float {
        val valid = iterations
            .asSequence()
            .mapNotNull { it.toLiftSetOrNull() }
            .toList()

        if (valid.isEmpty()) return 0f

        val working = pickWorkingSets(
            sets = valid,
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

    private data class LiftSet(
        val weight: Float,
        val reps: Int,
    )

    private fun Iteration.toLiftSetOrNull(): LiftSet? {
        val w = this.volume
        val r = this.repetitions
        if (!w.isFinite() || w <= EPS) return null
        if (r <= 0) return null
        return LiftSet(weight = w, reps = r)
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

    private fun computeStimulus(exercises: List<Exercise>): Float {
        var total = 0f
        exercises.forEach { exercise ->
            val s = exerciseStimulus(exercise.iterations)
            if (s.isFinite() && s > EPS) total += s
        }
        return if (total.isFinite() && total > EPS) total else 0f
    }

    private fun exerciseStimulus(iterations: List<Iteration>): Float {
        if (iterations.isEmpty()) return 0f

        val total = iterations.sumOf { iteration ->
            val reps = iteration.repetitions.coerceAtLeast(0)
            if (reps == 0) 0.0
            else {
                val w = iteration.volume.coerceAtLeast(0f)
                val intensity = 1f + (ln(1.0 + w.toDouble()).toFloat() / STIMULUS_LN_WEIGHT_SCALE)
                reps.toDouble() * intensity.toDouble()
            }
        }.toFloat()

        return if (total.isFinite() && total > EPS) total else 0f
    }

    private fun computeSpecializationTop2Percent(
        exercises: List<Exercise>,
        exampleMap: Map<String, ExerciseExample>,
    ): Float {
        val totals = mutableMapOf<MuscleEnum, Float>()

        exercises.forEach { exercise ->
            val exampleId = exercise.exerciseExample.id
            if (exampleId.isBlank()) return@forEach

            val bundles = exampleMap[exampleId]?.bundles ?: return@forEach
            if (bundles.isEmpty()) return@forEach

            val base = exerciseStimulus(exercise.iterations)
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
        exercises: List<Exercise>,
        category: CategoryEnum
    ): Float {
        var sum = 0f
        var matched = 0f

        exercises.forEach { e ->
            val w = exerciseStimulus(e.iterations)
            if (!w.isFinite() || w <= EPS) return@forEach

            sum += w
            if (e.exerciseExample.category == category) matched += w
        }

        if (!sum.isFinite() || sum <= EPS) return 0f
        val r = matched / sum
        return if (r.isFinite()) r.coerceIn(0f, 1f) else 0f
    }

    private fun weightedWeightTypeRatioByStimulus(
        exercises: List<Exercise>,
        type: WeightTypeEnum
    ): Float {
        var sum = 0f
        var matched = 0f

        exercises.forEach { e ->
            val w = exerciseStimulus(e.iterations)
            if (!w.isFinite() || w <= EPS) return@forEach

            sum += w
            if (e.exerciseExample.weightType == type) matched += w
        }

        if (!sum.isFinite() || sum <= EPS) return 0f
        val r = matched / sum
        return if (r.isFinite()) r.coerceIn(0f, 1f) else 0f
    }

    private fun weightedPushRatioByStimulus(exercises: List<Exercise>): Float {
        var sum = 0f
        var push = 0f

        exercises.forEach { e ->
            val w = exerciseStimulus(e.iterations)
            if (!w.isFinite() || w <= EPS) return@forEach

            sum += w
            val isPush = e.exerciseExample.forceType.toString().equals("push", ignoreCase = true)
            if (isPush) push += w
        }

        if (!sum.isFinite() || sum <= EPS) return 0f
        val r = push / sum
        return if (r.isFinite()) r.coerceIn(0f, 1f) else 0f
    }

    private data class AxisBand(
        val lo: Float,
        val hi: Float,
    )

    private class Normalizer(
        private val bands: Map<TrainingDimensionKind, AxisBand>,
    ) {
        fun score(kind: TrainingDimensionKind, raw: Float): Int {
            if (!raw.isFinite() || raw <= EPS) return 0

            val band = bands[kind] ?: return 0
            val lo = band.lo
            val hi = band.hi

            if (!lo.isFinite() || !hi.isFinite() || hi <= lo + EPS) return 50

            val x = log1pSafe(raw)
            val a = log1pSafe(lo)
            val b = log1pSafe(hi)
            if (b <= a + EPS) return 50

            val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
            return (t * 100f).toInt().coerceIn(0, 100)
        }
    }

    private fun buildNormalizer(reference: List<Raw>): Normalizer {
        fun band(values: List<Float>): AxisBand {
            val clean = values
                .filter { it.isFinite() && it > 0f }
                .sorted()

            if (clean.isEmpty()) return AxisBand(0f, 0f)
            if (clean.size == 1) {
                val v = clean.first()
                return AxisBand(v * 0.85f, v * 1.15f)
            }

            val p10 = percentileSorted(clean, 0.10f)
            val p90 = percentileSorted(clean, 0.90f)

            val lo = max(EPS, p10)
            val hi = max(lo + EPS, p90)

            return AxisBand(lo, hi)
        }

        val map = mapOf(
            TrainingDimensionKind.Strength to band(reference.map { it.strengthRaw }),
            TrainingDimensionKind.Hypertrophy to band(reference.map { it.hypertrophyRaw }),
            TrainingDimensionKind.Endurance to band(reference.map { it.enduranceRaw }),
        )

        return Normalizer(map)
    }

    private data class Scored(
        val strength: Int,
        val hypertrophy: Int,
        val endurance: Int,
        val compoundness: Int,
        val freeWeight: Int,
        val push: Int,
        val specialization: Int,
    )

    private fun computeScores(raw: Raw, normalizer: Normalizer): Scored {
        val strength = normalizer.score(TrainingDimensionKind.Strength, raw.strengthRaw)
        val hypertrophy = normalizer.score(TrainingDimensionKind.Hypertrophy, raw.hypertrophyRaw)
        val endurance = normalizer.score(TrainingDimensionKind.Endurance, raw.enduranceRaw)

        val compoundness = ratioToScore(raw.compoundRatio)
        val freeWeight = ratioToScore(raw.freeWeightRatio)
        val push = ratioToScore(raw.pushRatio)
        val specialization = percentToScore(raw.specializationTop2Percent)

        return Scored(
            strength = strength,
            hypertrophy = hypertrophy,
            endurance = endurance,
            compoundness = compoundness,
            freeWeight = freeWeight,
            push = push,
            specialization = specialization,
        )
    }

    private data class Rank(
        val strength: Float,
        val hypertrophy: Float,
        val endurance: Float,
        val compoundness: Float,
    )

    private class RanksByRaw(
        private val strengthSorted: List<Float>,
        private val hypertrophySorted: List<Float>,
        private val enduranceSorted: List<Float>,
        private val compoundSorted: List<Float>,
    ) {
        fun rankFor(raw: Raw): Rank {
            return Rank(
                strength = percentileRankFloat(strengthSorted, raw.strengthRaw),
                hypertrophy = percentileRankFloat(hypertrophySorted, raw.hypertrophyRaw),
                endurance = percentileRankFloat(enduranceSorted, raw.enduranceRaw),
                compoundness = percentileRankFloat(compoundSorted, raw.compoundRatio),
            )
        }
    }

    private fun buildRanksByRaw(reference: List<Raw>): RanksByRaw {
        fun sorted(values: List<Float>): List<Float> {
            return values.filter { it.isFinite() }.sorted()
        }

        return RanksByRaw(
            strengthSorted = sorted(reference.map { it.strengthRaw }),
            hypertrophySorted = sorted(reference.map { it.hypertrophyRaw }),
            enduranceSorted = sorted(reference.map { it.enduranceRaw }),
            compoundSorted = sorted(reference.map { it.compoundRatio }),
        )
    }

    private fun classifyWithinPool(
        raw: Raw,
        score: Scored,
        rank: Rank,
    ): TrainingProfileKind {
        if (raw.activity <= EASY_ACTIVITY_EPS) return TrainingProfileKind.Easy

        val ps = rank.strength
        val ph = rank.hypertrophy
        val pe = rank.endurance
        val pc = rank.compoundness

        val isPowerbuilding =
            ps >= POWERBUILDING_RANK_MIN &&
                    ph >= POWERBUILDING_RANK_MIN &&
                    abs(ps - ph) <= POWERBUILDING_RANK_GAP_MAX &&
                    pc >= POWERBUILDING_COMPOUND_RANK_MIN

        if (isPowerbuilding) return TrainingProfileKind.Powerbuilding

        val top = max(ps, max(ph, pe))
        val second = secondOfThree(ps, ph, pe)
        val gap = top - second

        if (top >= KIND_RANK_TOP_MIN && gap >= KIND_RANK_GAP_MIN) {
            return when (top) {
                ps -> TrainingProfileKind.Strength
                ph -> TrainingProfileKind.Hypertrophy
                else -> TrainingProfileKind.Endurance
            }
        }

        val looksStrength =
            score.specialization >= 70 && score.compoundness >= 70 && score.strength >= 45
        if (looksStrength) return TrainingProfileKind.Strength

        return TrainingProfileKind.Mixed
    }

    private fun secondOfThree(a: Float, b: Float, c: Float): Float {
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

    private fun ratioToScore(r: Float): Int {
        val v = if (r.isFinite()) r.coerceIn(0f, 1f) else 0f
        return (v * 100f).toInt().coerceIn(0, 100)
    }

    private fun percentToScore(p: Float): Int {
        val v = if (p.isFinite()) p.coerceIn(0f, 100f) else 0f
        return v.toInt().coerceIn(0, 100)
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

private fun percentileRankFloat(sorted: List<Float>, value: Float): Float {
    val n = sorted.size
    if (n <= 1) return 0.5f

    val v = if (value.isFinite()) value else 0f

    fun lowerBound(x: Float): Int {
        var lo = 0
        var hi = n
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sorted[mid] < x) lo = mid + 1 else hi = mid
        }
        return lo
    }

    fun upperBound(x: Float): Int {
        var lo = 0
        var hi = n
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sorted[mid] <= x) lo = mid + 1 else hi = mid
        }
        return lo
    }

    val lb = lowerBound(v)
    val ub = upperBound(v)

    val pos = if (lb >= ub) {
        lb.toFloat()
    } else {
        ((lb + (ub - 1)).toFloat()) * 0.5f
    }

    return (pos / (n - 1).toFloat()).coerceIn(0f, 1f)
}
