package com.grippo.data.features.api.training

import com.grippo.data.features.api.training.ExerciseValidatorUseCase.Companion.MAX_SAFE_RATIO
import com.grippo.data.features.api.training.ExerciseValidatorUseCase.Companion.MIN_SAFE_RATIO
import com.grippo.data.features.api.training.ExerciseValidatorUseCase.Companion.MIN_SAMPLES
import com.grippo.data.features.api.training.ExerciseValidatorUseCase.Companion.WARMUP_STEP_MIN_RATIO
import com.grippo.data.features.api.training.ExerciseValidatorUseCase.Companion.Z_THRESHOLD
import com.grippo.data.features.api.training.models.ExerciseArtifacts
import com.grippo.data.features.api.training.models.ExerciseArtifacts.Artifact
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.data.features.api.training.models.SetIteration
import kotlin.math.abs

/**
 * Detects likely input artifacts in a single [SetExercise] — values that look
 * like accidental typos (e.g. "10" instead of "100" kg).
 *
 * The detection is *soft*: it returns an [ExerciseArtifacts] so the UI can
 * surface a gentle hint to the user without blocking the save.
 *
 * The result contains the **full iteration list** with every iteration paired
 * with its optional [Artifact].  This lets the UI render the complete exercise
 * in context and highlight only the suspicious rows.
 *
 * Returns `null` when the exercise looks clean.
 *
 * ## Detection pipeline (in priority order)
 *
 * 1. **Zero/negative repetitions** — always wrong, flagged immediately with no
 *    minimum sample requirement.
 *
 * 2. **External weight** — analysed statistically when ≥ [MIN_SAMPLES]
 *    iterations carry a non-null, positive `externalWeight`.
 *
 * 3. **Repetitions** — analysed statistically when ≥ [MIN_SAMPLES] iterations
 *    exist.  An iteration already flagged in step 1 is skipped.
 *
 * 4. **Volume (fallback)** — analysed statistically when step 2 was skipped
 *    (not enough `externalWeight` data).  Covers bodyweight and
 *    assisted-bodyweight exercises.  Already-flagged iterations are skipped.
 *
 * ## Statistical algorithm
 *
 * Steps 2–4 use the **Modified Z-score** (Iglewicz & Hoaglin, 1993):
 *
 * ```
 * z_i = 0.6745 × |x_i − median| / MAD
 * ```
 *
 * where `MAD = median(|x_i − median|)`.
 *
 * This estimator is *robust*: a single bad value does not corrupt the
 * statistics used to detect it (unlike mean + stddev).
 *
 * When MAD ≈ 0 (all reference values are identical), the algorithm falls back
 * to a ratio comparison:
 * - ratio < [MIN_SAFE_RATIO]  →  suspiciously low (dropped a digit)
 * - ratio > [MAX_SAFE_RATIO]  →  suspiciously high (added a digit)
 *
 * ## Warmup suppression
 *
 * [Z_THRESHOLD] is raised to 5.0 (vs the textbook 3.5) and a monotonic-prefix
 * check is applied to avoid false positives on warmup pyramids such as
 * `20×5 40×5 60×5 80×5 100×5 100×5`.  A flagged value is suppressed when it
 * sits inside the non-decreasing warmup ramp AND the step to the next value is
 * gradual enough (ratio ≥ [WARMUP_STEP_MIN_RATIO]).
 *
 * ## Known limitations
 *
 * - If more than half of the iterations carry the same typo, the median shifts
 *   toward the wrong values and detection may fail.  In practice this is
 *   extremely unlikely.
 *
 * - When warmup sets and a typo appear in the same exercise, the inflated
 *   variance may prevent detection of the typo.
 */
public class ExerciseValidatorUseCase {

    private companion object {
        private const val MIN_SAMPLES = 3
        private const val Z_THRESHOLD = 5.0f
        private const val MAD_SCALE = 0.6745f
        private const val EPS = 1e-3f
        private const val MIN_SAFE_RATIO = 0.3f
        private const val MAX_SAFE_RATIO = 3.0f
        private const val WARMUP_STEP_MIN_RATIO = 0.2f
    }

    /**
     * Analyses [exercise] and returns an [ExerciseArtifacts] describing every
     * iteration paired with its optional [Artifact], or `null` if nothing
     * suspicious was found.
     */
    public fun execute(exercise: SetExercise): ExerciseArtifacts? {
        val iterations = exercise.iterations
        if (iterations.isEmpty()) return null

        // artifactMap: iterationIndex → Artifact (at most one per iteration)
        val artifactMap = mutableMapOf<Int, Artifact>()

        // ── Step 1: zero/negative repetitions ───────────────────────────────
        iterations.forEachIndexed { index, it ->
            if (it.repetitions <= 0) {
                val repMedian = medianOf(
                    iterations.mapIndexedNotNull { i, s ->
                        if (i != index && s.repetitions > 0) s.repetitions.toFloat() else null
                    }
                ) ?: 0f
                artifactMap[index] = Artifact.SuspiciousRepetitions(
                    actual = it.repetitions,
                    expectedMedian = repMedian,
                )
            }
        }

        // ── Step 2: external weight (statistical) ───────────────────────────
        val weightPairs: List<Pair<Int, Float>> = iterations
            .mapIndexedNotNull { index, it ->
                val w = it.externalWeight ?: return@mapIndexedNotNull null
                if (w <= EPS) return@mapIndexedNotNull null
                index to w
            }

        val weightCheckPerformed = weightPairs.size >= MIN_SAMPLES
        if (weightCheckPerformed) {
            val weightValues = weightPairs.map { it.second }
            val weightMedian = medianOf(weightValues) ?: 0f
            val outliers = suppressWarmupSets(findOutlierPositions(weightValues), weightValues)
            for (pos in outliers) {
                val (iterIndex, actual) = weightPairs[pos]
                if (iterIndex !in artifactMap) {
                    artifactMap[iterIndex] = Artifact.SuspiciousWeight(
                        actual = actual,
                        expectedMedian = weightMedian,
                    )
                }
            }
        }

        // ── Step 3: repetitions (statistical) ───────────────────────────────
        val repPairs: List<Pair<Int, Float>> = iterations
            .mapIndexedNotNull { index, it ->
                if (index in artifactMap) return@mapIndexedNotNull null
                if (it.repetitions <= 0) return@mapIndexedNotNull null
                index to it.repetitions.toFloat()
            }

        if (repPairs.size >= MIN_SAMPLES) {
            val repValues = repPairs.map { it.second }
            val repMedian = medianOf(repValues) ?: 0f
            for (pos in findOutlierPositions(repValues)) {
                val (iterIndex, actual) = repPairs[pos]
                if (iterIndex !in artifactMap) {
                    artifactMap[iterIndex] = Artifact.SuspiciousRepetitions(
                        actual = actual.toInt(),
                        expectedMedian = repMedian,
                    )
                }
            }
        }

        // ── Step 4: volume fallback (bodyweight / assisted exercises) ────────
        if (!weightCheckPerformed) {
            val volumePairs: List<Pair<Int, Float>> = iterations
                .mapIndexedNotNull { index, it ->
                    if (index in artifactMap) return@mapIndexedNotNull null
                    val hasAnyWeightField = it.externalWeight != null
                            || it.extraWeight != null
                            || it.bodyWeight != null
                            || it.assistWeight != null
                    if (!hasAnyWeightField) return@mapIndexedNotNull null
                    index to it.volume
                }

            if (volumePairs.size >= MIN_SAMPLES) {
                val volumeValues = volumePairs.map { it.second }
                val volumeMedian = medianOf(volumeValues) ?: 0f
                val outliers = suppressWarmupSets(findOutlierPositions(volumeValues), volumeValues)
                for (pos in outliers) {
                    val (iterIndex, actual) = volumePairs[pos]
                    if (iterIndex !in artifactMap) {
                        artifactMap[iterIndex] = Artifact.SuspiciousVolume(
                            actual = actual,
                            expectedMedian = volumeMedian,
                        )
                    }
                }
            }
        }

        if (artifactMap.isEmpty()) return null

        val pairedIterations: List<Pair<SetIteration, Artifact?>> = iterations
            .mapIndexed { index, iteration -> iteration to artifactMap[index] }

        return ExerciseArtifacts(
            exerciseName = exercise.name,
            iterations = pairedIterations,
        )
    }

    // ── Warmup suppression ───────────────────────────────────────────────────

    private fun suppressWarmupSets(
        outlierPositions: List<Int>,
        values: List<Float>,
    ): List<Int> {
        if (outlierPositions.isEmpty()) return emptyList()
        val med = medianOf(values) ?: return outlierPositions
        val prefixLen = monotonicPrefixLength(values)

        return outlierPositions.filter { pos ->
            val v = values[pos]
            if (pos >= prefixLen) return@filter true          // not in prefix → keep
            if (v >= med) return@filter true                  // above median → keep
            val next = values.getOrNull(pos + 1)
            if (next == null || next <= EPS) return@filter true
            v / next < WARMUP_STEP_MIN_RATIO                  // too big a jump → keep
        }
    }

    private fun monotonicPrefixLength(values: List<Float>): Int {
        var end = 0
        while (end + 1 < values.size && values[end + 1] >= values[end]) end++
        return end + 1
    }

    // ── Outlier detection ────────────────────────────────────────────────────

    private fun findOutlierPositions(values: List<Float>): List<Int> {
        if (values.size < MIN_SAMPLES) return emptyList()
        val med = medianOf(values) ?: return emptyList()
        if (abs(med) < EPS) return emptyList()

        val deviations = values.map { abs(it - med) }
        val mad = medianOf(deviations) ?: return emptyList()

        return if (mad < EPS) {
            values.indices.filter { i ->
                val ratio = values[i] / med
                ratio < MIN_SAFE_RATIO || ratio > MAX_SAFE_RATIO
            }
        } else {
            values.indices.filter { i ->
                val z = MAD_SCALE * abs(values[i] - med) / mad
                z > Z_THRESHOLD
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun medianOf(values: List<Float>): Float? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid]
        else (sorted[mid - 1] + sorted[mid]) / 2f
    }
}
