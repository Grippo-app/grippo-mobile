package com.grippo.data.features.api.metrics.performance

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.metrics.performance.models.ExerciseSpotlight
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

public class ExerciseSpotlightUseCase {

    private companion object {
        private const val MAX_INSIGHTS = 3
        private const val MIN_CONFIDENCE = 0.60f
        private const val COMPARISON_WINDOW_DAYS = 90
        private const val PERFORMANCE_MAX_RECENCY_DAYS = 21

        private const val PROGRESS_MIN_BASELINE_DAYS = 3
        private const val PROGRESS_MAX_BASELINE_DAYS = 5
        private const val PROGRESS_MAX_BASELINE_DEVIATION = 0.25
        private const val PROGRESS_TRIGGER_RATIO = 1.10

        private const val ATTENTION_MIN_DAYS = 4
        private const val ATTENTION_MAX_DAYS = 8
        private const val ATTENTION_MAX_TYPICAL_GAP = 21
        private const val ATTENTION_MAX_GAP_DEVIATION = 0.50
        private const val ATTENTION_MAX_ELAPSED_DAYS = 42

        private const val FREQUENCY_WINDOW_DAYS = 28
        private const val FREQUENCY_BUCKET_COUNT = 4
        private const val FREQUENCY_MIN_ACTIVE_WEEKS = 3
        private const val FREQUENCY_MIN_DAYS = 4
        private const val FREQUENCY_MAX_RECENCY_DAYS = 10

        private const val NEAR_BEST_MIN_PRIOR_DAYS = 2
        private const val NEAR_BEST_TRIGGER_RATIO = 0.95
    }

    /** [historyStart] is the earliest queried date; insights describe the available recorded sessions. */
    public fun buildCandidates(
        trainings: List<Training>,
        examples: List<ExerciseExample>,
        evaluatedAt: LocalDateTime,
        historyStart: LocalDate,
    ): List<ExerciseSpotlight> {
        require(historyStart <= evaluatedAt.date)

        val comparisonStart = maxOf(
            historyStart,
            evaluatedAt.date.minus(DatePeriod(days = COMPARISON_WINDOW_DAYS - 1)),
        )
        val comparisonWindowDays = comparisonStart.daysUntil(evaluatedAt.date) + 1
        val orderedTrainings = trainings
            .filter {
                it.id.isNotBlank() && it.createdAt.date >= comparisonStart && it.createdAt <= evaluatedAt
            }
            .groupBy { it.id }
            .mapNotNull { (_, copies) -> copies.distinct().singleOrNull() }
            .sortedWith(compareBy<Training> { it.createdAt }.thenBy { it.id })

        val componentsBySubject = examples.groupBy { it.value.id }
            .mapValues { (_, copies) -> copies.map { it.components }.distinct().singleOrNull() }
        val stats = collectStats(trainings = orderedTrainings, componentsBySubject = componentsBySubject)
        return buildList {
            for (stat in stats) {
                buildProgressWin(
                    stat = stat,
                    evaluatedAt = evaluatedAt,
                    comparisonWindowDays = comparisonWindowDays,
                )?.let(::add)
                buildNeedsAttention(stat = stat, evaluatedAt = evaluatedAt)?.let(::add)
                buildGoodFrequency(
                    stat = stat,
                    evaluatedAt = evaluatedAt,
                    historyStart = historyStart,
                )?.let(::add)
                buildNearBest(
                    stat = stat,
                    evaluatedAt = evaluatedAt,
                    comparisonWindowDays = comparisonWindowDays,
                )?.let(::add)
            }
        }.sortedWith(spotlightOrder)
    }

    public fun buildSpotlights(
        trainings: List<Training>,
        examples: List<ExerciseExample>,
        evaluatedAt: LocalDateTime,
        historyStart: LocalDate,
        maxInsights: Int = MAX_INSIGHTS,
    ): List<ExerciseSpotlight> {
        require(maxInsights >= 0)
        if (maxInsights == 0) {
            return emptyList()
        }

        val candidates = buildCandidates(
            trainings = trainings,
            examples = examples,
            evaluatedAt = evaluatedAt,
            historyStart = historyStart,
        )
            .filter { it.confidence >= MIN_CONFIDENCE }
        val selected = ArrayList<ExerciseSpotlight>()

        for (candidate in candidates) {
            if (selected.size == maxInsights) {
                break
            }
            if (selected.any { it.example.id == candidate.example.id }) {
                continue
            }
            if (candidate.severity == ExerciseSpotlight.Severity.Warning &&
                selected.any { it.severity == ExerciseSpotlight.Severity.Warning }
            ) {
                continue
            }
            selected += candidate
        }
        return selected
    }

    private fun buildProgressWin(
        stat: SubjectStats,
        evaluatedAt: LocalDateTime,
        comparisonWindowDays: Int,
    ): ExerciseSpotlight.ProgressWin? {
        val latest = stat.sessions.last()
        val latestVolume = latest.workload ?: return null
        val recencyDays = latest.at.date.daysUntil(evaluatedAt.date)
        if (recencyDays > PERFORMANCE_MAX_RECENCY_DAYS) {
            return null
        }

        val baselineSessions = stat.sessions
            .filter { it.at.date < latest.at.date }
            .groupBy { it.at.date }
            .values
            .map { it.last() }
            .filter { it.workload != null }
            .takeLast(PROGRESS_MAX_BASELINE_DAYS)
        if (baselineSessions.size < PROGRESS_MIN_BASELINE_DAYS) {
            return null
        }

        val baselineVolumes = baselineSessions.mapNotNull { it.workload }
        val baselineMedian = median(baselineVolumes)
        val maxDeviation = baselineVolumes.maxOf { abs(it - baselineMedian) / baselineMedian }
        if (maxDeviation > PROGRESS_MAX_BASELINE_DEVIATION) {
            return null
        }

        val ratio = latestVolume / baselineMedian
        if (!ratio.isFinite() || ratio < PROGRESS_TRIGGER_RATIO) {
            return null
        }
        val improvement = (ratio - 1.0) * 100.0
        if (improvement > Int.MAX_VALUE.toDouble()) {
            return null
        }

        val support = sampleFactor(count = baselineSessions.size, fullAt = PROGRESS_MAX_BASELINE_DAYS)
        val recency = recencyFactor(days = recencyDays, maxDays = PERFORMANCE_MAX_RECENCY_DAYS)
        val stability = (1.0 - maxDeviation).toFloat()
        val progressSignal = ((ratio - 1.0) / 0.35).toFloat().coerceIn(0f, 1f)
        return ExerciseSpotlight.ProgressWin(
            example = stat.example,
            confidence = (support * 0.50f + stability * 0.30f + recency * 0.20f).coerceIn(0f, 1f),
            score = (progressSignal * 0.55f + support * 0.25f + recency * 0.20f).coerceIn(0f, 1f),
            lastSeenAt = latest.at,
            baselineVolumeMedian = baselineMedian.toFloat(),
            latestSessionVolume = latestVolume.toFloat(),
            improvementPercent = improvement.roundToInt(),
            comparedSessions = baselineSessions.size,
            comparisonWindowDays = comparisonWindowDays,
        )
    }

    private fun buildNeedsAttention(
        stat: SubjectStats,
        evaluatedAt: LocalDateTime,
    ): ExerciseSpotlight.NeedsAttention? {
        val completed = stat.sessions.filter { it.hasCompletedSets }
        val dates = completed.map { it.at.date }.distinct().takeLast(ATTENTION_MAX_DAYS)
        if (dates.size < ATTENTION_MIN_DAYS) {
            return null
        }

        val gaps = dates.zipWithNext { previous, next -> previous.daysUntil(next).toDouble() }
        val typicalGap = median(gaps)
        if (typicalGap > ATTENTION_MAX_TYPICAL_GAP) {
            return null
        }
        val maxDeviation = gaps.maxOf { abs(it - typicalGap) / typicalGap }
        if (maxDeviation > ATTENTION_MAX_GAP_DEVIATION) {
            return null
        }

        val roundedTypicalGap = typicalGap.roundToInt()
        val triggerGap = maxOf(ceil(typicalGap * 1.5).toInt(), roundedTypicalGap + 3, 7)
        val expiryGap = minOf(
            ATTENTION_MAX_ELAPSED_DAYS,
            maxOf(triggerGap + 7, ceil(typicalGap * 3.0).toInt()),
        )
        val lastSeenAt = completed.last().at
        val currentGap = lastSeenAt.date.daysUntil(evaluatedAt.date)
        if (currentGap < triggerGap || currentGap > expiryGap) {
            return null
        }

        val support = sampleFactor(count = dates.size, fullAt = ATTENTION_MAX_DAYS)
        val stability = (1.0 - maxDeviation).toFloat()
        val freshness = (expiryGap - currentGap).toFloat() / (expiryGap - triggerGap).toFloat()
        val overdue = currentGap.toFloat() / expiryGap.toFloat()
        return ExerciseSpotlight.NeedsAttention(
            example = stat.example,
            confidence = (support * 0.35f + stability * 0.40f + freshness * 0.25f).coerceIn(0f, 1f),
            score = (overdue * 0.45f + support * 0.20f + freshness * 0.35f).coerceIn(0f, 1f),
            sampleSize = dates.size,
            lastSeenAt = lastSeenAt,
            typicalGapDays = roundedTypicalGap,
            currentGapDays = currentGap,
            triggerGapDays = triggerGap,
        )
    }

    private fun buildGoodFrequency(
        stat: SubjectStats,
        evaluatedAt: LocalDateTime,
        historyStart: LocalDate,
    ): ExerciseSpotlight.GoodFrequency? {
        val windowStart = evaluatedAt.date.minus(DatePeriod(days = FREQUENCY_WINDOW_DAYS - 1))
        if (historyStart > windowStart) {
            return null
        }

        val completed = stat.sessions.filter { it.hasCompletedSets && it.at.date >= windowStart }
        val dates = completed.map { it.at.date }.distinct()
        if (dates.size < FREQUENCY_MIN_DAYS) {
            return null
        }
        val lastSeenAt = completed.last().at
        val recencyDays = lastSeenAt.date.daysUntil(evaluatedAt.date)
        if (recencyDays > FREQUENCY_MAX_RECENCY_DAYS) {
            return null
        }

        val weeklyCounts = IntArray(FREQUENCY_BUCKET_COUNT)
        for (date in dates) {
            weeklyCounts[date.daysUntil(evaluatedAt.date) / 7] += 1
        }
        val activeWeeks = weeklyCounts.count { it > 0 }
        if (activeWeeks < FREQUENCY_MIN_ACTIVE_WEEKS) {
            return null
        }

        val average = dates.size.toFloat() / FREQUENCY_BUCKET_COUNT
        val coverage = activeWeeks.toFloat() / FREQUENCY_BUCKET_COUNT
        val support = sampleFactor(count = dates.size, fullAt = 8)
        val recency = recencyFactor(days = recencyDays, maxDays = FREQUENCY_MAX_RECENCY_DAYS)
        val frequencySignal = (average / 2f).coerceIn(0f, 1f)
        return ExerciseSpotlight.GoodFrequency(
            example = stat.example,
            confidence = (coverage * 0.50f + support * 0.30f + recency * 0.20f).coerceIn(0f, 1f),
            score = (frequencySignal * 0.40f + coverage * 0.35f + recency * 0.25f).coerceIn(0f, 1f),
            lastSeenAt = lastSeenAt,
            appearancesInWindow = dates.size,
            activeWeeks = activeWeeks,
            avgWeeklyFrequency = average,
            recentWindowDays = FREQUENCY_WINDOW_DAYS,
        )
    }

    private fun buildNearBest(
        stat: SubjectStats,
        evaluatedAt: LocalDateTime,
        comparisonWindowDays: Int,
    ): ExerciseSpotlight.NearBest? {
        val latest = stat.sessions.last()
        val latestVolume = latest.workload ?: return null
        val recencyDays = latest.at.date.daysUntil(evaluatedAt.date)
        if (recencyDays > PERFORMANCE_MAX_RECENCY_DAYS) {
            return null
        }

        val prior = stat.sessions.dropLast(1).filter { it.workload != null }
        val priorDayCount = prior.map { it.at.date }.distinct().size
        if (priorDayCount < NEAR_BEST_MIN_PRIOR_DAYS) {
            return null
        }
        val best = prior.mapNotNull { it.workload }.max()
        val closeness = latestVolume / best
        if (closeness < NEAR_BEST_TRIGGER_RATIO || closeness > 1.0) {
            return null
        }

        val support = sampleFactor(count = priorDayCount, fullAt = 5)
        val recency = recencyFactor(days = recencyDays, maxDays = PERFORMANCE_MAX_RECENCY_DAYS)
        val closenessSignal = ((closeness - NEAR_BEST_TRIGGER_RATIO) / (1.0 - NEAR_BEST_TRIGGER_RATIO))
            .toFloat()
        return ExerciseSpotlight.NearBest(
            example = stat.example,
            confidence = (support * 0.60f + recency * 0.40f).coerceIn(0f, 1f),
            score = (closenessSignal * 0.55f + support * 0.20f + recency * 0.25f).coerceIn(0f, 1f),
            lastSeenAt = latest.at,
            bestSessionVolume = best.toFloat(),
            latestSessionVolume = latestVolume.toFloat(),
            gapPercent = ((1.0 - closeness) * 100.0).roundToInt(),
            comparedSessions = prior.size,
            comparisonWindowDays = comparisonWindowDays,
        )
    }

    private fun collectStats(
        trainings: List<Training>,
        componentsBySubject: Map<String, ExerciseExampleComponents?>,
    ): List<SubjectStats> {
        val bySubject = LinkedHashMap<String, MutableList<SubjectSession>>()
        val examples = LinkedHashMap<String, ExerciseExampleValue>()

        for (training in trainings) {
            val subjects = training.exercises.groupBy { it.exerciseExample.id }
            for ((subjectId, exercises) in subjects) {
                if (subjectId.isBlank()) {
                    continue
                }
                val example = exercises.last().exerciseExample
                examples[subjectId] = example
                val session = collectSession(
                    at = training.createdAt,
                    exercises = exercises,
                    components = componentsBySubject[subjectId],
                )
                bySubject.getOrPut(subjectId) { ArrayList() } += session
            }
        }
        return bySubject.map { (subjectId, sessions) ->
            SubjectStats(example = examples.getValue(subjectId), sessions = sessions)
        }
    }

    private fun collectSession(
        at: LocalDateTime,
        exercises: List<Exercise>,
        components: ExerciseExampleComponents?,
    ): SubjectSession {
        var completeWorkload = components != null
        val hasCompletedSets = exercises.any { exercise ->
            exercise.iterations.any(::hasCompletedRepetitions)
        }
        var workload = 0.0
        val seenExercises = HashSet<String>()
        val seenIterations = HashSet<String>()

        for (exercise in exercises) {
            if (exercise.id.isBlank() || !seenExercises.add(exercise.id) || exercise.iterations.isEmpty()) {
                completeWorkload = false
                continue
            }
            for (iteration in exercise.iterations) {
                if (iteration.id.isBlank() || !seenIterations.add(iteration.id)) {
                    completeWorkload = false
                    continue
                }
                val load = components?.let { recordedLoad(iteration = iteration, components = it) }
                if (load == null) {
                    completeWorkload = false
                    continue
                }
                if (load <= 0.0) {
                    completeWorkload = false
                    continue
                }
                workload += load * iteration.repetitions.toDouble()
            }
        }
        val measuredWorkload = workload.takeIf {
            completeWorkload && hasCompletedSets && it.isFinite() && it > 0.0 &&
                it <= Float.MAX_VALUE.toDouble() && it.toFloat() > 0f
        }
        return SubjectSession(at = at, hasCompletedSets = hasCompletedSets, workload = measuredWorkload)
    }

    private fun hasCompletedRepetitions(iteration: Iteration): Boolean {
        return iteration.repetitions > 0
    }

    private fun recordedLoad(iteration: Iteration, components: ExerciseExampleComponents): Double? {
        if (!hasCompletedRepetitions(iteration)) {
            return null
        }
        val recordedComponents = listOf(
            iteration.externalWeight,
            iteration.extraWeight,
            iteration.assistWeight,
            iteration.bodyWeight,
            iteration.bodyMultiplier,
        )
        if (recordedComponents.any { it != null && (!it.isFinite() || it < 0f) }) {
            return null
        }

        return when (components) {
            is ExerciseExampleComponents.External -> {
                if (iteration.bodyWeight != null || iteration.bodyMultiplier != null ||
                    iteration.extraWeight != null || iteration.assistWeight != null
                ) {
                    return null
                }
                val external = iteration.externalWeight?.toDouble()
                if (components.required && (external == null || external <= 0.0)) {
                    return null
                }
                external
            }

            is ExerciseExampleComponents.BodyOnly -> {
                if (iteration.externalWeight != null || iteration.extraWeight != null ||
                    iteration.assistWeight != null
                ) {
                    return null
                }
                recordedBodyLoad(iteration)
            }

            is ExerciseExampleComponents.BodyAndExtra -> {
                if (iteration.externalWeight != null || iteration.assistWeight != null) {
                    return null
                }
                val body = recordedBodyLoad(iteration) ?: return null
                if (components.extraRequired && iteration.extraWeight == null) {
                    return null
                }
                val extra = iteration.extraWeight?.toDouble() ?: 0.0
                body + extra
            }

            is ExerciseExampleComponents.BodyAndAssist -> {
                if (iteration.externalWeight != null || iteration.extraWeight != null) {
                    return null
                }
                val body = recordedBodyLoad(iteration) ?: return null
                if (components.assistRequired && iteration.assistWeight == null) {
                    return null
                }
                val assist = iteration.assistWeight?.toDouble() ?: 0.0
                body - assist
            }
        }
    }

    private fun recordedBodyLoad(iteration: Iteration): Double? {
        // Optional entry fields do not establish a measured zero body contribution.
        val body = iteration.bodyWeight ?: return null
        val multiplier = iteration.bodyMultiplier ?: return null
        if (body <= 0f || multiplier <= 0f) {
            return null
        }
        // Preserve the recorded Float product so full assistance cancels exactly.
        val bodyLoad = body * multiplier
        if (!bodyLoad.isFinite() || bodyLoad <= 0f) {
            return null
        }
        return bodyLoad.toDouble()
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            sorted[middle - 1] / 2.0 + sorted[middle] / 2.0
        }
    }

    private fun sampleFactor(count: Int, fullAt: Int): Float {
        return (count.toFloat() / fullAt).coerceIn(0f, 1f)
    }

    private fun recencyFactor(days: Int, maxDays: Int): Float {
        return (1f - days.toFloat() / maxDays).coerceIn(0f, 1f)
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
            .thenBy { it.example.name }
            .thenBy { it.example.id }

    private data class SubjectSession(
        val at: LocalDateTime,
        val hasCompletedSets: Boolean,
        val workload: Double?,
    )

    private data class SubjectStats(
        val example: ExerciseExampleValue,
        val sessions: List<SubjectSession>,
    )
}
