package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.metrics.models.ExerciseSpotlight
import com.grippo.data.features.api.training.models.Training
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

public class ExerciseSpotlightUseCase {

    private companion object {
        private const val EPS: Float = 1e-3f
        private const val MAX_INSIGHTS: Int = 3
        private const val MIN_CONFIDENCE: Float = 0.60f

        private const val PROGRESS_MIN_APPEARANCES: Int = 4
        private const val PROGRESS_FULL_CONFIDENCE_APPEARANCES: Int = 6
        private const val PROGRESS_MAX_RECENCY_DAYS: Int = 21
        private const val PROGRESS_TRIGGER_RATIO: Float = 1.10f

        private const val ATTENTION_MIN_APPEARANCES: Int = 3

        private const val FREQUENCY_WINDOW_DAYS: Int = 28
        private const val FREQUENCY_BUCKET_COUNT: Int = 4
        private const val FREQUENCY_MIN_ACTIVE_WEEKS: Int = 3
        private const val FREQUENCY_MIN_APPEARANCES: Int = 4
        private const val FREQUENCY_MIN_WEEKLY_AVG: Float = 1f
        private const val FREQUENCY_MAX_RECENCY_DAYS: Int = 10

        private const val NEAR_BEST_MIN_APPEARANCES: Int = 3
        private const val NEAR_BEST_MAX_RECENCY_DAYS: Int = 21
        private const val NEAR_BEST_TRIGGER_RATIO: Float = 0.95f
    }

    public fun buildCandidates(trainings: List<Training>): List<ExerciseSpotlight> {
        val orderedTrainings = trainings.sortedBy { it.createdAt }
        if (orderedTrainings.isEmpty()) return emptyList()

        val stats = collectStats(orderedTrainings)
        if (stats.isEmpty()) return emptyList()

        val freshestTrainingAt = orderedTrainings.last().createdAt

        return buildList {
            addAll(buildProgressWins(stats.values, freshestTrainingAt))
            addAll(buildNeedsAttention(stats.values, freshestTrainingAt))
            addAll(buildGoodFrequency(stats.values, freshestTrainingAt))
            addAll(buildNearBest(stats.values, freshestTrainingAt))
        }.sortedWith(spotlightOrder)
    }

    public fun buildSpotlights(
        trainings: List<Training>,
        maxInsights: Int = MAX_INSIGHTS,
    ): List<ExerciseSpotlight> {
        if (maxInsights <= 0) return emptyList()

        val filtered = buildCandidates(trainings)
            .filter { it.confidence >= MIN_CONFIDENCE }

        if (filtered.isEmpty()) return emptyList()

        val deduplicatedBySubject = filtered
            .groupBy { it.subjectId }
            .mapNotNull { (_, items) -> items.sortedWith(spotlightOrder).firstOrNull() }

        if (deduplicatedBySubject.isEmpty()) return emptyList()

        val selected = LinkedHashMap<String, ExerciseSpotlight>(maxInsights)

        deduplicatedBySubject
            .filter { it.severity == ExerciseSpotlight.Severity.Warning }
            .sortedWith(spotlightOrder)
            .firstOrNull()
            ?.let { selected[it.subjectId] = it }

        deduplicatedBySubject
            .filter { it.severity == ExerciseSpotlight.Severity.Positive }
            .sortedWith(spotlightOrder)
            .firstOrNull()
            ?.let { candidate ->
                if (selected.containsKey(candidate.subjectId).not()) {
                    selected[candidate.subjectId] = candidate
                }
            }

        deduplicatedBySubject
            .asSequence()
            .sortedWith(spotlightOrder)
            .filter { candidate ->
                selected.containsKey(candidate.subjectId).not() &&
                        candidate.severity != ExerciseSpotlight.Severity.Warning
            }
            .take(max(0, maxInsights - selected.size))
            .forEach { selected[it.subjectId] = it }

        return selected.values
            .sortedWith(spotlightOrder)
            .take(maxInsights)
    }

    private fun buildProgressWins(
        stats: Collection<SubjectStats>,
        freshestTrainingAt: LocalDateTime,
    ): List<ExerciseSpotlight.ProgressWin> {
        return stats.mapNotNull { stat ->
            val sessions = stat.sessions
            if (sessions.size < PROGRESS_MIN_APPEARANCES) return@mapNotNull null

            val latest = sessions.last()
            val baselineSessions = sessions
                .dropLast(1)
                .takeLast(min(5, sessions.size - 1))

            if (baselineSessions.size < 3) return@mapNotNull null

            val baselineMedian = medianFloat(baselineSessions.map { it.volume })
            if (baselineMedian <= EPS) return@mapNotNull null

            val ratio = latest.volume / baselineMedian
            if (ratio < PROGRESS_TRIGGER_RATIO) return@mapNotNull null

            val recencyDays = daysBetween(latest.at.date, freshestTrainingAt.date)
            if (recencyDays > PROGRESS_MAX_RECENCY_DAYS) return@mapNotNull null

            val sampleFactor = sampleFactor(
                count = sessions.size,
                fullAt = PROGRESS_FULL_CONFIDENCE_APPEARANCES,
            )
            val recencyFactor = recencyFactor(recencyDays, PROGRESS_MAX_RECENCY_DAYS)
            val baselineSupport = (baselineSessions.size.toFloat() / 5f).coerceIn(0f, 1f)
            val progressSignal = ((ratio - 1f) / 0.35f).coerceIn(0f, 1f)

            val confidence = weighted(
                sampleFactor to 0.45f,
                recencyFactor to 0.35f,
                baselineSupport to 0.20f,
            )

            val score = weighted(
                progressSignal to 0.55f,
                sampleFactor to 0.25f,
                recencyFactor to 0.20f,
            )

            ExerciseSpotlight.ProgressWin(
                subjectId = stat.subjectId,
                subjectLabel = stat.subjectLabel,
                example = stat.example,
                confidence = confidence,
                score = score,
                sampleSize = sessions.size,
                lastSeenAt = latest.at,
                baselineVolumeMedian = baselineMedian,
                latestSessionVolume = latest.volume,
                improvementPercent = ((ratio - 1f) * 100f).roundToInt(),
            )
        }
    }

    private fun buildNeedsAttention(
        stats: Collection<SubjectStats>,
        freshestTrainingAt: LocalDateTime,
    ): List<ExerciseSpotlight.NeedsAttention> {
        return stats.mapNotNull { stat ->
            val appearanceDates = stat.appearanceDates()
            if (appearanceDates.size < ATTENTION_MIN_APPEARANCES) return@mapNotNull null

            val gaps = buildDayGaps(appearanceDates)
            if (gaps.isEmpty()) return@mapNotNull null

            val typicalGap = medianFloat(gaps.map(Int::toFloat))
            if (typicalGap <= EPS) return@mapNotNull null

            val roundedTypicalGap = typicalGap.roundToInt().coerceAtLeast(1)
            val triggerGapDays = max(
                max(ceil((typicalGap * 1.5f).toDouble()).toInt(), roundedTypicalGap + 3),
                7,
            )

            val lastSeenAt = stat.sessions.last().at
            val currentGapDays = daysBetween(lastSeenAt.date, freshestTrainingAt.date)
            if (currentGapDays <= 0 || currentGapDays < triggerGapDays) return@mapNotNull null

            val sampleFactor = sampleFactor(appearanceDates.size, fullAt = 7)
            val cadenceStability = gapStabilityFactor(gaps, typicalGap)
            val recencyFactor = overdueRecencyFactor(
                currentGapDays = currentGapDays,
                triggerGapDays = triggerGapDays,
            )
            val overdueSignal = ((currentGapDays.toFloat() / triggerGapDays.toFloat()) / 2f)
                .coerceIn(0f, 1f)

            val confidence = weighted(
                sampleFactor to 0.35f,
                cadenceStability to 0.40f,
                recencyFactor to 0.25f,
            )

            val score = weighted(
                overdueSignal to 0.55f,
                sampleFactor to 0.20f,
                recencyFactor to 0.25f,
            )

            ExerciseSpotlight.NeedsAttention(
                subjectId = stat.subjectId,
                subjectLabel = stat.subjectLabel,
                example = stat.example,
                confidence = confidence,
                score = score,
                sampleSize = appearanceDates.size,
                lastSeenAt = lastSeenAt,
                typicalGapDays = roundedTypicalGap,
                currentGapDays = currentGapDays,
                triggerGapDays = triggerGapDays,
            )
        }
    }

    private fun buildGoodFrequency(
        stats: Collection<SubjectStats>,
        freshestTrainingAt: LocalDateTime,
    ): List<ExerciseSpotlight.GoodFrequency> {
        val freshestDate = freshestTrainingAt.date
        val windowStart = freshestDate.minus(DatePeriod(days = FREQUENCY_WINDOW_DAYS - 1))

        return stats.mapNotNull { stat ->
            val windowDates = stat.appearanceDates()
                .filter { date -> date >= windowStart && date <= freshestDate }

            if (windowDates.isEmpty()) return@mapNotNull null

            val weeklyCounts = IntArray(FREQUENCY_BUCKET_COUNT)
            for (date in windowDates) {
                val distance = daysBetween(date, freshestDate)
                if (distance !in 0 until FREQUENCY_WINDOW_DAYS) continue
                val bucket = (FREQUENCY_BUCKET_COUNT - 1) - (distance / 7)
                if (bucket in weeklyCounts.indices) {
                    weeklyCounts[bucket] += 1
                }
            }

            val appearancesInWindow = weeklyCounts.sum()
            val activeWeeks = weeklyCounts.count { it > 0 }
            val avgWeeklyFrequency =
                appearancesInWindow.toFloat() / FREQUENCY_BUCKET_COUNT.toFloat()
            val recencyDays = daysBetween(stat.sessions.last().at.date, freshestDate)

            if (activeWeeks < FREQUENCY_MIN_ACTIVE_WEEKS &&
                appearancesInWindow < FREQUENCY_MIN_APPEARANCES
            ) {
                return@mapNotNull null
            }

            if (avgWeeklyFrequency < FREQUENCY_MIN_WEEKLY_AVG) return@mapNotNull null
            if (recencyDays > FREQUENCY_MAX_RECENCY_DAYS) return@mapNotNull null
            if (weeklyCounts.count { it == 0 } > 1) return@mapNotNull null

            val sampleFactor = sampleFactor(appearancesInWindow, fullAt = 8)
            val recencyFactor = recencyFactor(recencyDays, FREQUENCY_MAX_RECENCY_DAYS)
            val stability = frequencyStabilityFactor(weeklyCounts)
            val coverageFactor = activeWeeks.toFloat() / FREQUENCY_BUCKET_COUNT.toFloat()
            val frequencySignal = (avgWeeklyFrequency / 2f).coerceIn(0f, 1f)

            val confidence = weighted(
                coverageFactor to 0.40f,
                stability to 0.30f,
                recencyFactor to 0.20f,
                sampleFactor to 0.10f,
            )

            val score = weighted(
                frequencySignal to 0.45f,
                coverageFactor to 0.30f,
                recencyFactor to 0.15f,
                sampleFactor to 0.10f,
            )

            ExerciseSpotlight.GoodFrequency(
                subjectId = stat.subjectId,
                subjectLabel = stat.subjectLabel,
                example = stat.example,
                confidence = confidence,
                score = score,
                sampleSize = appearancesInWindow,
                lastSeenAt = stat.sessions.last().at,
                appearancesInWindow = appearancesInWindow,
                activeWeeks = activeWeeks,
                avgWeeklyFrequency = avgWeeklyFrequency,
                recentWindowDays = FREQUENCY_WINDOW_DAYS,
            )
        }
    }

    private fun buildNearBest(
        stats: Collection<SubjectStats>,
        freshestTrainingAt: LocalDateTime,
    ): List<ExerciseSpotlight.NearBest> {
        return stats.mapNotNull { stat ->
            val sessions = stat.sessions
            if (sessions.size < NEAR_BEST_MIN_APPEARANCES) return@mapNotNull null

            val latest = sessions.last()
            val best = sessions.maxOfOrNull { it.volume } ?: return@mapNotNull null
            if (best <= EPS) return@mapNotNull null

            val closeness = latest.volume / best
            if (closeness < NEAR_BEST_TRIGGER_RATIO) return@mapNotNull null

            val recencyDays = daysBetween(latest.at.date, freshestTrainingAt.date)
            if (recencyDays > NEAR_BEST_MAX_RECENCY_DAYS) return@mapNotNull null

            val sampleFactor = sampleFactor(sessions.size, fullAt = 8)
            val recencyFactor = recencyFactor(recencyDays, NEAR_BEST_MAX_RECENCY_DAYS)
            val closenessSignal = ((closeness - 0.90f) / 0.10f).coerceIn(0f, 1f)

            val confidence = weighted(
                sampleFactor to 0.50f,
                recencyFactor to 0.50f,
            )

            val score = weighted(
                closenessSignal to 0.55f,
                sampleFactor to 0.20f,
                recencyFactor to 0.25f,
            )

            ExerciseSpotlight.NearBest(
                subjectId = stat.subjectId,
                subjectLabel = stat.subjectLabel,
                example = stat.example,
                confidence = confidence,
                score = score,
                sampleSize = sessions.size,
                lastSeenAt = latest.at,
                bestSessionVolume = best,
                latestSessionVolume = latest.volume,
                gapPercent = ((1f - closeness) * 100f).roundToInt().coerceAtLeast(0),
            )
        }
    }

    private fun collectStats(trainings: List<Training>): Map<String, SubjectStats> {
        val acc = LinkedHashMap<String, SubjectStatsBuilder>()

        for (training in trainings) {
            val perTraining = LinkedHashMap<String, PerTrainingAgg>()

            for (exercise in training.exercises) {
                val volume = exercise.volume
                if (volume <= EPS || exercise.repetitions <= 0) continue

                val example = exercise.exerciseExample
                val subjectId = example.id

                val current = perTraining[subjectId]
                if (current == null) {
                    perTraining[subjectId] = PerTrainingAgg(
                        subjectId = subjectId,
                        subjectLabel = example.name,
                        example = example,
                        volume = volume,
                    )
                } else {
                    perTraining[subjectId] = current.copy(
                        volume = current.volume + volume,
                    )
                }
            }

            for (aggregate in perTraining.values) {
                val builder = acc.getOrPut(aggregate.subjectId) {
                    SubjectStatsBuilder(
                        subjectId = aggregate.subjectId,
                        subjectLabel = aggregate.subjectLabel,
                        example = aggregate.example,
                    )
                }

                builder.sessions += SubjectSession(
                    at = training.createdAt,
                    volume = aggregate.volume,
                )
            }
        }

        return acc.mapValues { (_, builder) -> builder.build() }
    }

    private fun daysBetween(from: LocalDate, to: LocalDate): Int {
        return from.daysUntil(to).coerceAtLeast(0)
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

    private fun buildDayGaps(dates: List<LocalDate>): List<Int> {
        if (dates.size < 2) return emptyList()

        val gaps = ArrayList<Int>(dates.size - 1)
        for (index in 1 until dates.size) {
            val gap = daysBetween(dates[index - 1], dates[index])
            if (gap > 0) {
                gaps += gap
            }
        }
        return gaps
    }

    private fun sampleFactor(count: Int, fullAt: Int): Float {
        return (count.toFloat() / fullAt.toFloat()).coerceIn(0f, 1f)
    }

    private fun recencyFactor(days: Int, maxDays: Int): Float {
        return (1f - (days.toFloat() / maxDays.toFloat())).coerceIn(0f, 1f)
    }

    private fun overdueRecencyFactor(
        currentGapDays: Int,
        triggerGapDays: Int,
    ): Float {
        val extraGapDays = max(0, currentGapDays - triggerGapDays)
        val fadeWindow = triggerGapDays + 28
        return (1f - (extraGapDays.toFloat() / fadeWindow.toFloat())).coerceIn(0f, 1f)
    }

    private fun gapStabilityFactor(
        gaps: List<Int>,
        typicalGap: Float,
    ): Float {
        if (gaps.isEmpty() || typicalGap <= EPS) return 0f

        val averageDeviation = gaps
            .map { gap -> abs(gap.toFloat() - typicalGap) }
            .average()
            .toFloat()

        return (1f - (averageDeviation / typicalGap)).coerceIn(0f, 1f)
    }

    private fun frequencyStabilityFactor(weeklyCounts: IntArray): Float {
        val activeCounts = weeklyCounts.filter { it > 0 }
        if (activeCounts.isEmpty()) return 0f

        val minActive = activeCounts.minOrNull() ?: 0
        val maxCount = weeklyCounts.maxOrNull() ?: 0
        val spreadPenalty = ((maxCount - minActive).toFloat() / 3f).coerceIn(0f, 1f)
        val zeroPenalty = if (weeklyCounts.count { it == 0 } == 0) 1f else 0.85f

        return ((1f - spreadPenalty) * zeroPenalty).coerceIn(0f, 1f)
    }

    private fun weighted(vararg parts: Pair<Float, Float>): Float {
        if (parts.isEmpty()) return 0f

        var weightedSum = 0f
        var weightSum = 0f

        for ((value, weight) in parts) {
            weightedSum += value.coerceIn(0f, 1f) * weight
            weightSum += weight
        }

        if (weightSum <= EPS) return 0f
        return (weightedSum / weightSum).coerceIn(0f, 1f)
    }

    private fun businessPriority(kind: ExerciseSpotlight.Kind): Int {
        return when (kind) {
            ExerciseSpotlight.Kind.NeedsAttention -> 4
            ExerciseSpotlight.Kind.ProgressWin -> 3
            ExerciseSpotlight.Kind.NearBest -> 2
            ExerciseSpotlight.Kind.GoodFrequency -> 1
        }
    }

    private val spotlightOrder: Comparator<ExerciseSpotlight> =
        compareByDescending<ExerciseSpotlight> { businessPriority(it.kind) }
            .thenByDescending { it.score }
            .thenByDescending { it.confidence }
            .thenByDescending { it.sampleSize }
            .thenBy { it.subjectLabel }

    private data class PerTrainingAgg(
        val subjectId: String,
        val subjectLabel: String,
        val example: ExerciseExampleValue,
        val volume: Float,
    )

    private data class SubjectSession(
        val at: LocalDateTime,
        val volume: Float,
    )

    private data class SubjectStats(
        val subjectId: String,
        val subjectLabel: String,
        val example: ExerciseExampleValue,
        val sessions: List<SubjectSession>,
    ) {
        fun appearanceDates(): List<LocalDate> {
            return sessions
                .map { it.at.date }
                .distinct()
        }
    }

    private data class SubjectStatsBuilder(
        val subjectId: String,
        val subjectLabel: String,
        val example: ExerciseExampleValue,
        val sessions: MutableList<SubjectSession> = mutableListOf(),
    ) {
        fun build(): SubjectStats = SubjectStats(
            subjectId = subjectId,
            subjectLabel = subjectLabel,
            example = example,
            sessions = sessions.toList(),
        )
    }
}
