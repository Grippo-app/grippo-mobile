package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.metrics.models.ExerciseSpotlight
import com.grippo.data.features.api.training.models.Training
import kotlin.math.min

public class ExerciseSpotlightUseCase {

    private companion object {
        private const val EPS = 1e-3f
        private const val BASELINE_SESSIONS = 3
    }

    public fun mostConsistent(trainings: List<Training>): ExerciseSpotlight.MostConsistent? {
        if (trainings.isEmpty()) return null

        val stats = collectStats(trainings)
        if (stats.isEmpty()) return null

        val trainingsCount = trainings.size

        val best = stats.values.maxWithOrNull(
            compareBy<ExampleStats> { it.sessionCount }
                .thenBy { it.totalVolume }
                .thenBy { it.example.name }
        ) ?: return null

        val coverageRatio = best.sessionCount.toFloat() / trainingsCount.toFloat()

        return ExerciseSpotlight.MostConsistent(
            example = best.example,
            totalVolume = best.totalVolume,
            sessionCount = best.sessionCount,
            trainingsCount = trainingsCount,
            coverageRatio = coverageRatio,
        )
    }

    public fun bestProgress(trainings: List<Training>): ExerciseSpotlight.BestProgress? {
        if (trainings.isEmpty()) return null

        val stats = collectStats(trainings)
        if (stats.isEmpty()) return null

        val candidates = stats.values.mapNotNull { s ->
            val sessions = s.sessionVolumes
            if (sessions.size < 2) return@mapNotNull null

            val last = sessions.last()
            if (last <= EPS) return@mapNotNull null

            val baselineCount = min(BASELINE_SESSIONS, sessions.size - 1)
            val baselineSlice = sessions.dropLast(1).takeLast(baselineCount)
            if (baselineSlice.isEmpty()) return@mapNotNull null

            val baselineMedian = medianFloat(baselineSlice)
            if (baselineMedian <= EPS) return@mapNotNull null

            val delta = last - baselineMedian
            val ratio = delta / baselineMedian
            if (ratio <= 0f) return@mapNotNull null

            ProgressCandidate(
                stats = s,
                baselineMedian = baselineMedian,
                last = last,
                delta = delta,
                ratio = ratio
            )
        }

        if (candidates.isEmpty()) return null

        val best = candidates.maxWithOrNull(
            compareBy<ProgressCandidate> { it.ratio }
                .thenBy { it.delta }
                .thenBy { it.stats.sessionCount }
                .thenBy { it.stats.totalVolume }
                .thenBy { it.stats.example.name }
        ) ?: return null

        return ExerciseSpotlight.BestProgress(
            example = best.stats.example,
            totalVolume = best.stats.totalVolume,
            sessionCount = best.stats.sessionCount,
            baselineVolumeMedian = best.baselineMedian,
            lastSessionVolume = best.last,
            progressDelta = best.delta,
            progressRatio = best.ratio,
        )
    }

    public fun comebackMissing(trainings: List<Training>): ExerciseSpotlight.ComebackMissing? {
        if (trainings.isEmpty()) return null

        val stats = collectStats(trainings)
        if (stats.isEmpty()) return null

        val lastTrainingIndex = trainings.lastIndex

        val candidates = stats.values.mapNotNull { s ->
            val indices = s.trainingIndices
            if (indices.size < 2) return@mapNotNull null

            val gaps = buildGaps(indices)
            if (gaps.isEmpty()) return@mapNotNull null

            val typicalGap = medianInt(gaps)
            if (typicalGap <= EPS) return@mapNotNull null

            val lastSeenIndex = indices.last()
            val currentGap = lastTrainingIndex - lastSeenIndex
            if (currentGap <= 0) return@mapNotNull null

            val score = currentGap.toFloat() / typicalGap
            if (score <= 1f) return@mapNotNull null

            MissingCandidate(
                stats = s,
                typicalGap = typicalGap,
                currentGap = currentGap,
                score = score
            )
        }

        if (candidates.isEmpty()) return null

        val best = candidates.maxWithOrNull(
            compareBy<MissingCandidate> { it.score }
                .thenBy { it.currentGap }
                .thenBy { it.stats.sessionCount }
                .thenBy { it.stats.totalVolume }
                .thenBy { it.stats.example.name }
        ) ?: return null

        return ExerciseSpotlight.ComebackMissing(
            example = best.stats.example,
            totalVolume = best.stats.totalVolume,
            sessionCount = best.stats.sessionCount,
            typicalGap = best.typicalGap,
            currentGap = best.currentGap,
            score = best.score,
        )
    }

    private fun collectStats(trainings: List<Training>): Map<String, ExampleStats> {
        val acc = LinkedHashMap<String, ExampleStatsBuilder>()

        for ((trainingIndex, training) in trainings.withIndex()) {
            val perTraining = LinkedHashMap<String, PerTrainingAgg>()

            for (exercise in training.exercises) {
                val volume = exercise.volume
                if (volume <= EPS) continue

                val reps = exercise.repetitions
                if (reps <= 0) continue

                val example = exercise.exerciseExample
                val exampleId = example.id

                val agg = perTraining[exampleId]
                if (agg == null) {
                    perTraining[exampleId] = PerTrainingAgg(
                        example = example,
                        volume = volume
                    )
                } else {
                    perTraining[exampleId] = agg.copy(
                        volume = agg.volume + volume
                    )
                }
            }

            for ((exampleId, agg) in perTraining) {
                val b = acc.getOrPut(exampleId) { ExampleStatsBuilder(example = agg.example) }
                b.totalVolume += agg.volume
                b.sessionCount += 1
                b.trainingIndices.add(trainingIndex)
                b.sessionVolumes.add(agg.volume)
            }
        }

        return acc.mapValues { (_, b) -> b.build() }
    }

    private fun buildGaps(indices: List<Int>): List<Int> {
        if (indices.size < 2) return emptyList()
        val gaps = ArrayList<Int>(indices.size - 1)
        for (i in 1 until indices.size) {
            val g = indices[i] - indices[i - 1]
            if (g > 0) gaps.add(g)
        }
        return gaps
    }

    private fun medianInt(values: List<Int>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid].toFloat()
        } else {
            (sorted[mid - 1] + sorted[mid]).toFloat() / 2f
        }
    }

    private fun medianFloat(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2f
        }
    }

    private data class PerTrainingAgg(
        val example: ExerciseExampleValue,
        val volume: Float,
    )

    private data class ExampleStats(
        val example: ExerciseExampleValue,
        val totalVolume: Float,
        val sessionCount: Int,
        val trainingIndices: List<Int>,
        val sessionVolumes: List<Float>,
    )

    private data class ExampleStatsBuilder(
        val example: ExerciseExampleValue,
        var totalVolume: Float = 0f,
        var sessionCount: Int = 0,
        val trainingIndices: MutableList<Int> = mutableListOf(),
        val sessionVolumes: MutableList<Float> = mutableListOf(),
    ) {
        fun build(): ExampleStats = ExampleStats(
            example = example,
            totalVolume = totalVolume,
            sessionCount = sessionCount,
            trainingIndices = trainingIndices,
            sessionVolumes = sessionVolumes,
        )
    }

    private data class ProgressCandidate(
        val stats: ExampleStats,
        val baselineMedian: Float,
        val last: Float,
        val delta: Float,
        val ratio: Float,
    )

    private data class MissingCandidate(
        val stats: ExampleStats,
        val typicalGap: Float,
        val currentGap: Int,
        val score: Float,
    )
}
