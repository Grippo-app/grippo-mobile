package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.metrics.models.TrainingStreak
import com.grippo.data.features.api.metrics.models.TrainingStreakFeatured
import com.grippo.data.features.api.metrics.models.TrainingStreakKind
import com.grippo.data.features.api.metrics.models.TrainingStreakMood
import com.grippo.data.features.api.metrics.models.TrainingStreakProgressEntry
import com.grippo.data.features.api.training.models.Training
import com.grippo.data.features.api.user.UserFeature
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.abs
import kotlin.math.roundToInt

public class TrainingStreakUseCase(
    private val userFeature: UserFeature,
) {

    private companion object {
        private const val RHYTHM_TOLERANCE_DAYS = 1
        private const val RHYTHM_MAX_WORK_DAYS = 5
        private const val RHYTHM_MAX_REST_DAYS = 4
        private const val RHYTHM_MAX_CYCLE_LENGTH = 8

        private const val DEFAULT_PATTERN_MAX_PERIOD_DAYS = 28
        private const val MIN_WEEKS_FOR_HISTORY_TARGET = 3
        private const val RECENT_WEEKS_FOR_TARGET = 8

        private const val PAUSED_GAP_FROM_DAYS = 2
        private const val RESTART_GAP_FROM_DAYS = 14

        private const val STARTUP_HISTORY_DAYS = 10
        private const val MATURE_HISTORY_DAYS = 28
    }

    /**
     * previousKind is optional stabilization input.
     * Store it from the previous TrainingStreak.kind and pass back on next recomputation.
     */
    public suspend fun fromTrainings(
        trainings: List<Training>,
    ): TrainingStreak {
        val today = DateTimeUtils.now().date

        val sessionsByDay = trainings.groupingBy { it.createdAt.date }.eachCount()

        val dates = sessionsByDay.keys
        val lastTrainingDate = dates.maxOrNull()
        val earliestTrainingDate = dates.minOrNull()

        val lastTrainingGapDays = when {
            lastTrainingDate == null -> -1
            else -> lastTrainingDate.daysUntil(today).coerceAtLeast(0)
        }

        val historyDays = when {
            earliestTrainingDate == null -> 0
            else -> (earliestTrainingDate.daysUntil(today) + 1).coerceAtLeast(1)
        }

        if (sessionsByDay.isEmpty()) {
            return emptyStreak(
                today = today,
                kind = TrainingStreakKind.Daily,
                score = 0f,
                historyDays = historyDays,
                lastTrainingGapDays = lastTrainingGapDays
            )
        }

        val experience = resolveExperience() ?: ExperienceEnum.BEGINNER
        val adaptation = experience.trainingStreakAdaptation()

        val weekBuckets = trainings.buildWeekBuckets()
        val weeklyData = weekBuckets.weeklyData(today = today)
        val dailyData = sessionsByDay.dailyData(today)

        val dayBlocks = sessionsByDay.keys.sorted().toDayBlocks()

        val thresholdRelax = confidenceRelaxation(historyDays)
        val rhythmMinConfidence =
            (adaptation.rhythmMinConfidence - thresholdRelax).coerceIn(0.35f, 1f)
        val patternMinConfidence =
            (adaptation.patternMinConfidence - thresholdRelax).coerceIn(0.4f, 1f)

        val rhythmMinBlocks = when {
            historyDays < 21 -> maxOf(2, adaptation.rhythmMinBlocks - 1)
            else -> adaptation.rhythmMinBlocks
        }

        val patternMinCycles = when {
            historyDays < 28 -> maxOf(2, adaptation.patternMinCycles - 1)
            else -> adaptation.patternMinCycles
        }

        val rhythmPattern = detectRhythmPattern(
            blocks = dayBlocks,
            minBlocks = rhythmMinBlocks,
            minConfidence = rhythmMinConfidence,
        )

        val patternCandidate = computePatternCandidate(
            sessionsByDay = sessionsByDay,
            today = today,
            horizonDays = adaptation.patternHorizonDays,
            maxPeriodDays = DEFAULT_PATTERN_MAX_PERIOD_DAYS,
            minCycles = patternMinCycles,
            minConfidence = patternMinConfidence,
            historyDays = historyDays,
            lastTrainingGapDays = lastTrainingGapDays,
        )

        val candidates = buildList {
            add(
                buildDailyCandidate(
                    dailyData = dailyData,
                    today = today,
                    sessionsByDay = sessionsByDay,
                    historyDays = historyDays,
                    lastTrainingGapDays = lastTrainingGapDays,
                )
            )

            val weeklyCandidate = buildWeeklyCandidateOrNull(
                weeklyData = weeklyData,
                today = today,
                historyDays = historyDays,
                lastTrainingGapDays = lastTrainingGapDays,
            )
            if (weeklyCandidate != null) add(weeklyCandidate)

            val rhythmCandidate = buildRhythmCandidateOrNull(
                pattern = rhythmPattern,
                blocks = dayBlocks,
                historyDays = historyDays,
                lastTrainingGapDays = lastTrainingGapDays,
            )
            if (rhythmCandidate != null) add(rhythmCandidate)

            if (patternCandidate != null) add(patternCandidate)
        }

        val best = candidates.maxBy { it.score }

        return TrainingStreak(
            totalActiveDays = sessionsByDay.size,
            featured = best.featured,
            timeline = best.timeline,
            kind = best.kind,
            score = best.score,
            historyDays = historyDays,
            lastTrainingGapDays = lastTrainingGapDays,
        )
    }

    private fun confidenceRelaxation(historyDays: Int): Float {
        return when {
            historyDays < 7 -> 0.12f
            historyDays < 14 -> 0.08f
            historyDays < 21 -> 0.05f
            else -> 0f
        }
    }

    private fun emptyStreak(
        today: LocalDate,
        kind: TrainingStreakKind,
        score: Float,
        historyDays: Int,
        lastTrainingGapDays: Int,
    ): TrainingStreak {
        return TrainingStreak(
            totalActiveDays = 0,
            featured = TrainingStreakFeatured.Daily(
                length = 0,
                mood = TrainingStreakMood.Restart,
                progressPercent = 0,
                confidence = 0f,
            ),
            timeline = buildDailyTimeline(today, emptyMap()),
            kind = kind,
            score = score.coerceIn(0f, 1f),
            historyDays = historyDays.coerceAtLeast(0),
            lastTrainingGapDays = lastTrainingGapDays,
        )
    }

    private data class Candidate(
        val kind: TrainingStreakKind,
        val featured: TrainingStreakFeatured,
        val timeline: List<TrainingStreakProgressEntry>,
        val score: Float,
    )

    private fun scoreCandidate(
        kind: TrainingStreakKind,
        confidence: Float,
        historyDays: Int,
        lastTrainingGapDays: Int,
        progressPercent: Int,
    ): Float {
        val baseWeight = when (kind) {
            TrainingStreakKind.Daily -> 0.86f
            TrainingStreakKind.Weekly -> 0.92f
            TrainingStreakKind.Rhythm -> 1.00f
            TrainingStreakKind.Pattern -> 1.00f
        }

        val historyBonus = when {
            historyDays < STARTUP_HISTORY_DAYS -> when (kind) {
                TrainingStreakKind.Daily -> 0.10f
                TrainingStreakKind.Weekly -> 0.05f
                TrainingStreakKind.Rhythm -> -0.15f
                TrainingStreakKind.Pattern -> -0.20f
            }

            historyDays < MATURE_HISTORY_DAYS -> when (kind) {
                TrainingStreakKind.Daily -> 0.00f
                TrainingStreakKind.Weekly -> 0.05f
                TrainingStreakKind.Rhythm -> -0.05f
                TrainingStreakKind.Pattern -> -0.10f
            }

            else -> when (kind) {
                TrainingStreakKind.Daily -> -0.05f
                TrainingStreakKind.Weekly -> 0.02f
                TrainingStreakKind.Rhythm -> 0.05f
                TrainingStreakKind.Pattern -> 0.08f
            }
        }

        val progressBonus = (progressPercent.coerceIn(0, 100) / 100f) * 0.05f

        val recencyFactor = when {
            lastTrainingGapDays < 0 -> 1f
            lastTrainingGapDays <= 1 -> 1f
            lastTrainingGapDays <= 6 -> 0.85f
            lastTrainingGapDays <= 13 -> 0.70f
            else -> 0.55f
        }

        val raw = (confidence.coerceIn(0f, 1f) * baseWeight) + historyBonus + progressBonus
        return (raw * recencyFactor).coerceIn(0f, 1f)
    }

    private fun moodFor(
        length: Int,
        progressPercent: Int,
        lastTrainingGapDays: Int,
    ): TrainingStreakMood {
        if (lastTrainingGapDays < 0) return TrainingStreakMood.Restart
        if (lastTrainingGapDays >= RESTART_GAP_FROM_DAYS) return TrainingStreakMood.Restart
        if (lastTrainingGapDays >= PAUSED_GAP_FROM_DAYS) return TrainingStreakMood.Paused

        return when {
            length >= 4 -> TrainingStreakMood.CrushingIt
            length >= 1 -> TrainingStreakMood.OnTrack
            progressPercent > 0 -> TrainingStreakMood.OnTrack
            else -> TrainingStreakMood.Restart
        }
    }

    private fun buildDailyCandidate(
        dailyData: DailyStreakData,
        today: LocalDate,
        sessionsByDay: Map<LocalDate, Int>,
        historyDays: Int,
        lastTrainingGapDays: Int,
    ): Candidate {
        val confidenceBase = (sessionsByDay.size.toFloat() / 7f).coerceIn(0f, 1f)
        val confidence =
            (confidenceBase * historyFactor(historyDays) * recencyFactor(lastTrainingGapDays))
                .coerceIn(0f, 1f)

        val mood = moodFor(
            length = dailyData.currentStreak,
            progressPercent = dailyData.progressPercent,
            lastTrainingGapDays = lastTrainingGapDays,
        )

        val featured = TrainingStreakFeatured.Daily(
            length = dailyData.currentStreak,
            mood = mood,
            progressPercent = dailyData.progressPercent,
            confidence = confidence,
        )

        val timeline = buildDailyTimeline(today = today, dayCounts = sessionsByDay)

        val score = scoreCandidate(
            kind = TrainingStreakKind.Daily,
            confidence = confidence,
            historyDays = historyDays,
            lastTrainingGapDays = lastTrainingGapDays,
            progressPercent = dailyData.progressPercent,
        )

        return Candidate(
            kind = TrainingStreakKind.Daily,
            featured = featured,
            timeline = timeline,
            score = score,
        )
    }

    private fun buildWeeklyCandidateOrNull(
        weeklyData: WeeklyStreakData,
        today: LocalDate,
        historyDays: Int,
        lastTrainingGapDays: Int,
    ): Candidate? {
        if (weeklyData.targetSessions <= 0) return null

        val confidenceBase = (weeklyData.countsByWeekStart.size.toFloat() / 4f).coerceIn(0f, 1f)
        val confidence =
            (confidenceBase * historyFactor(historyDays) * recencyFactor(lastTrainingGapDays))
                .coerceIn(0f, 1f)

        val mood = moodFor(
            length = weeklyData.streakLength,
            progressPercent = weeklyData.progressPercent,
            lastTrainingGapDays = lastTrainingGapDays,
        )

        val featured = TrainingStreakFeatured.Weekly(
            length = weeklyData.streakLength,
            targetSessionsPerWeek = weeklyData.targetSessions,
            mood = mood,
            progressPercent = weeklyData.progressPercent,
            confidence = confidence,
        )

        val timeline = buildWeeklyTimeline(
            today = today,
            countsByWeekStart = weeklyData.countsByWeekStart,
            target = weeklyData.targetSessions,
        )

        val score = scoreCandidate(
            kind = TrainingStreakKind.Weekly,
            confidence = confidence,
            historyDays = historyDays,
            lastTrainingGapDays = lastTrainingGapDays,
            progressPercent = weeklyData.progressPercent,
        )

        return Candidate(
            kind = TrainingStreakKind.Weekly,
            featured = featured,
            timeline = timeline,
            score = score,
        )
    }

    private fun buildRhythmCandidateOrNull(
        pattern: RhythmPattern?,
        blocks: List<DayBlock>,
        historyDays: Int,
        lastTrainingGapDays: Int,
    ): Candidate? {
        if (pattern == null) return null
        if (blocks.isEmpty()) return null

        val latestBlock = blocks.last()
        val blockProgress =
            ((latestBlock.length.coerceAtMost(pattern.workDays).toFloat() / pattern.workDays)
                .coerceIn(0f, 1f) * 100).roundToInt()

        var pointer = blocks.lastIndex
        if (blocks[pointer].length < pattern.workDays) pointer--

        var streak = 0
        var index = pointer
        while (index >= 0) {
            val block = blocks[index]
            if (block.length < pattern.workDays) break

            streak++

            val previousIndex = index - 1
            if (previousIndex < 0) break

            val restGap = restBetween(blocks[previousIndex], block)
            if (restGap <= pattern.restDays + RHYTHM_TOLERANCE_DAYS) {
                index--
            } else {
                break
            }
        }

        val confidence =
            (pattern.confidence * blocksFactor(blocks.size) * historyFactor(historyDays) * recencyFactor(
                lastTrainingGapDays
            ))
                .coerceIn(0f, 1f)

        val mood = moodFor(
            length = streak,
            progressPercent = blockProgress,
            lastTrainingGapDays = lastTrainingGapDays,
        )

        val featured = TrainingStreakFeatured.Rhythm(
            length = streak,
            workDays = pattern.workDays,
            restDays = pattern.restDays,
            mood = mood,
            progressPercent = blockProgress,
            confidence = confidence,
        )

        val timeline = buildRhythmTimeline(blocks = blocks, pattern = pattern)

        val score = scoreCandidate(
            kind = TrainingStreakKind.Rhythm,
            confidence = confidence,
            historyDays = historyDays,
            lastTrainingGapDays = lastTrainingGapDays,
            progressPercent = blockProgress,
        )

        return Candidate(
            kind = TrainingStreakKind.Rhythm,
            featured = featured,
            timeline = timeline,
            score = score,
        )
    }

    private fun computePatternCandidate(
        sessionsByDay: Map<LocalDate, Int>,
        today: LocalDate,
        horizonDays: Int,
        maxPeriodDays: Int,
        minCycles: Int,
        minConfidence: Float,
        historyDays: Int,
        lastTrainingGapDays: Int,
    ): Candidate? {
        if (sessionsByDay.isEmpty()) return null

        val series = (0 until horizonDays).map { offset ->
            val date = today.minus(DatePeriod(days = offset))
            (sessionsByDay[date] ?: 0) > 0
        }

        if (series.count { it } < 3) return null

        val candidate = detectBestPattern(
            series = series,
            maxPeriodDays = maxPeriodDays,
            minCycles = minCycles,
        ) ?: return null

        if (candidate.confidence < minConfidence) return null

        val target = candidate.expectedTrainingDays
        if (target <= 0) return null

        fun cycleIndex(dayIndex: Int): Int = (dayIndex + candidate.shift) / candidate.periodDays
        fun posIndex(dayIndex: Int): Int = (dayIndex + candidate.shift) % candidate.periodDays

        val cycles = (0 until horizonDays).groupBy(::cycleIndex)
        val currentCycleDays = cycles.getValue(0)

        val achievedInCurrent = currentCycleDays.count { idx ->
            series[idx] && candidate.mask[posIndex(idx)]
        }
        val currentProgress =
            ((achievedInCurrent.toFloat() / target.toFloat()).coerceIn(0f, 1f) * 100).roundToInt()

        val streakCycles = generateSequence(1) { it + 1 }
            .mapNotNull { c -> cycles[c]?.let { c to it } }
            .takeWhile { (_, indices) -> indices.size >= candidate.periodDays }
            .takeWhile { (_, indices) ->
                val achieved = indices.count { idx -> series[idx] && candidate.mask[posIndex(idx)] }
                achieved >= target
            }
            .count()

        val timeline = (3 downTo 0).mapNotNull { cycle ->
            val indices = cycles[cycle] ?: return@mapNotNull null
            if (indices.isEmpty()) return@mapNotNull null
            val achieved = indices.count { idx -> series[idx] && candidate.mask[posIndex(idx)] }
            val percent =
                ((achieved.toFloat() / target.toFloat()).coerceIn(0f, 1f) * 100).roundToInt()
            TrainingStreakProgressEntry(
                progressPercent = percent,
                achievedSessions = achieved,
                targetSessions = target,
            )
        }

        val confidence =
            (candidate.confidence * historyFactor(historyDays) * recencyFactor(lastTrainingGapDays))
                .coerceIn(0f, 1f)

        val mood = moodFor(
            length = streakCycles,
            progressPercent = currentProgress,
            lastTrainingGapDays = lastTrainingGapDays,
        )

        val featured = TrainingStreakFeatured.Pattern(
            length = streakCycles,
            targetSessionsPerPeriod = target,
            periodLengthDays = candidate.periodDays,
            mask = candidate.mask,
            mood = mood,
            progressPercent = currentProgress,
            confidence = confidence,
        )

        val score = scoreCandidate(
            kind = TrainingStreakKind.Pattern,
            confidence = confidence,
            historyDays = historyDays,
            lastTrainingGapDays = lastTrainingGapDays,
            progressPercent = currentProgress,
        )

        return Candidate(
            kind = TrainingStreakKind.Pattern,
            featured = featured,
            timeline = timeline,
            score = score,
        )
    }

    private fun historyFactor(historyDays: Int): Float {
        if (historyDays <= 0) return 0f
        return (historyDays.toFloat() / 28f).coerceIn(0.35f, 1f)
    }

    private fun blocksFactor(blocksCount: Int): Float {
        if (blocksCount <= 0) return 0f
        return (blocksCount.toFloat() / 4f).coerceIn(0.4f, 1f)
    }

    private fun recencyFactor(lastTrainingGapDays: Int): Float {
        return when {
            lastTrainingGapDays < 0 -> 0.3f
            lastTrainingGapDays <= 1 -> 1f
            lastTrainingGapDays <= 6 -> 0.85f
            lastTrainingGapDays <= 13 -> 0.70f
            else -> 0.55f
        }
    }

    private data class RhythmPattern(
        val workDays: Int,
        val restDays: Int,
        val confidence: Float,
    )

    private data class DayBlock(
        val start: LocalDate,
        val length: Int,
    ) {
        val end: LocalDate = start.plus(DatePeriod(days = length - 1))
    }

    private fun Collection<LocalDate>.toDayBlocks(): List<DayBlock> {
        if (isEmpty()) return emptyList()
        val sorted = sorted()
        val blocks = mutableListOf<DayBlock>()
        var start = sorted.first()
        var length = 1
        for (index in 1 until sorted.size) {
            val current = sorted[index]
            val previous = sorted[index - 1]
            if (current == previous.plus(DatePeriod(days = 1))) {
                length++
            } else {
                blocks += DayBlock(start = start, length = length)
                start = current
                length = 1
            }
        }
        blocks += DayBlock(start = start, length = length)
        return blocks
    }

    private fun detectRhythmPattern(
        blocks: List<DayBlock>,
        minBlocks: Int,
        minConfidence: Float,
    ): RhythmPattern? {
        val confidenceThreshold = minConfidence.coerceIn(0f, 1f)
        val effectiveMinBlocks = minBlocks.coerceAtLeast(1)
        if (blocks.size < effectiveMinBlocks) return null

        val runLengths = blocks.map { it.length }
        val restLengths = blocks.restLengths()
        if (restLengths.size < effectiveMinBlocks - 1) return null

        val runMode = runLengths.modeOrNull() ?: return null
        val restMode = restLengths.modeOrNull() ?: return null

        if (runMode.value < 2 || restMode.value <= 0) return null

        val runConfidence = runMode.count.toFloat() / runLengths.size
        val restConfidence = restMode.count.toFloat() / restLengths.size
        val multiDayFraction = runLengths.count { it >= 2 }.toFloat() / runLengths.size

        if (runConfidence < confidenceThreshold ||
            restConfidence < confidenceThreshold ||
            multiDayFraction < 0.4f
        ) {
            return null
        }

        val workDays = runMode.value.coerceIn(2, RHYTHM_MAX_WORK_DAYS)
        val restDays = restMode.value.coerceIn(1, RHYTHM_MAX_REST_DAYS)
        if (workDays + restDays > RHYTHM_MAX_CYCLE_LENGTH) return null

        val confidence = (runConfidence * restConfidence * multiDayFraction).coerceIn(0f, 1f)
        return RhythmPattern(workDays = workDays, restDays = restDays, confidence = confidence)
    }

    private data class ModeValue(
        val value: Int,
        val count: Int,
    )

    private fun List<Int>.modeOrNull(): ModeValue? {
        if (isEmpty()) return null
        val stats = groupingBy { it }.eachCount()
        val entry = stats.entries.maxByOrNull { it.value } ?: return null
        return ModeValue(value = entry.key, count = entry.value)
    }

    private fun List<DayBlock>.restLengths(): List<Int> {
        if (size < 2) return emptyList()
        val rests = mutableListOf<Int>()
        for (index in 0 until size - 1) {
            val current = this[index]
            val next = this[index + 1]
            val rest = restBetween(current, next)
            if (rest > 0) rests += rest
        }
        return rests
    }

    private fun restBetween(previous: DayBlock, next: DayBlock): Int {
        val gap = previous.end.daysUntil(next.start) - 1
        return gap.coerceAtLeast(0)
    }

    private fun buildRhythmTimeline(
        blocks: List<DayBlock>,
        pattern: RhythmPattern,
        periods: Int = 4,
    ): List<TrainingStreakProgressEntry> {
        if (blocks.isEmpty()) return emptyList()
        val recent = blocks.takeLast(periods)
        val entries = recent.map { block ->
            val achieved = block.length.coerceAtMost(pattern.workDays)
            val percent =
                ((achieved.toFloat() / pattern.workDays).coerceIn(0f, 1f) * 100).roundToInt()
            TrainingStreakProgressEntry(
                progressPercent = percent,
                achievedSessions = achieved,
                targetSessions = pattern.workDays
            )
        }
        if (entries.size >= periods) return entries
        val padding = List(periods - entries.size) {
            TrainingStreakProgressEntry(
                progressPercent = 0,
                achievedSessions = 0,
                targetSessions = pattern.workDays
            )
        }
        return padding + entries
    }

    private data class WeekBucket(
        val start: LocalDate,
        val count: Int,
    )

    private fun List<Training>.buildWeekBuckets(): List<WeekBucket> {
        if (isEmpty()) return emptyList()
        val buckets = mutableMapOf<LocalDate, Int>()
        for (training in this) {
            val date = training.createdAt.date
            val daysFromMonday = date.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber
            val weekStart = date.minus(DatePeriod(days = daysFromMonday))
            buckets[weekStart] = (buckets[weekStart] ?: 0) + 1
        }
        return buckets.entries
            .sortedBy { it.key }
            .map { WeekBucket(start = it.key, count = it.value) }
    }

    private data class WeeklyStreakData(
        val targetSessions: Int,
        val streakLength: Int,
        val currentWeekCount: Int,
        val countsByWeekStart: Map<LocalDate, Int>,
    ) {
        val progressPercent: Int =
            if (targetSessions <= 0) 0
            else ((currentWeekCount.toFloat() / targetSessions).coerceIn(0f, 1f) * 100).roundToInt()
    }

    private fun List<WeekBucket>.weeklyData(today: LocalDate): WeeklyStreakData {
        if (isEmpty()) {
            return WeeklyStreakData(
                targetSessions = 0,
                streakLength = 0,
                currentWeekCount = 0,
                countsByWeekStart = emptyMap()
            )
        }

        val countsByWeek = associate { it.start to it.count }
        val weeklyTarget = deriveWeeklyTarget()

        val currentWeekStart = today.minus(
            DatePeriod(days = today.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber)
        )
        val latestWeekStart = maxOf(last().start, currentWeekStart)

        val streakLength = computeWeeklyStreakLength(
            countsByWeekStart = countsByWeek,
            currentWeekStart = currentWeekStart,
            latestWeekStart = latestWeekStart,
            target = weeklyTarget
        )

        val currentWeekCount = countsByWeek[currentWeekStart] ?: 0

        return WeeklyStreakData(
            targetSessions = weeklyTarget,
            streakLength = streakLength,
            currentWeekCount = currentWeekCount,
            countsByWeekStart = countsByWeek
        )
    }

    private fun computeWeeklyStreakLength(
        countsByWeekStart: Map<LocalDate, Int>,
        currentWeekStart: LocalDate,
        latestWeekStart: LocalDate,
        target: Int,
    ): Int {
        if (target <= 0 || countsByWeekStart.isEmpty()) return 0
        val earliest = countsByWeekStart.keys.minOrNull() ?: return 0
        var pointer = latestWeekStart
        var streak = 0
        var firstIteration = true

        while (pointer >= earliest) {
            val weekCount = countsByWeekStart[pointer] ?: 0
            if (firstIteration && pointer == currentWeekStart && weekCount < target) {
                pointer = pointer.minus(DatePeriod(days = 7))
                firstIteration = false
                continue
            }
            firstIteration = false

            if (weekCount >= target) {
                streak++
                pointer = pointer.minus(DatePeriod(days = 7))
            } else {
                break
            }
        }

        return streak
    }

    private data class DailyStreakData(
        val currentStreak: Int,
        val progressPercent: Int,
    )

    private fun Map<LocalDate, Int>.dailyData(today: LocalDate): DailyStreakData {
        if (isEmpty()) return DailyStreakData(currentStreak = 0, progressPercent = 0)

        val sortedDates = keys.sorted()
        var streak = 1

        var lastDate = sortedDates.last()
        for (index in sortedDates.size - 2 downTo 0) {
            val candidate = sortedDates[index]
            if (candidate == lastDate.minus(DatePeriod(days = 1))) {
                streak++
                lastDate = candidate
            } else {
                break
            }
        }

        val lastTraining = sortedDates.last()
        val gapFromToday = lastTraining.daysUntil(today)
        if (gapFromToday > 1) streak = 0

        val todaySessions = this[today] ?: 0
        val progress = if (todaySessions > 0) 100 else 0

        return DailyStreakData(
            currentStreak = streak.coerceAtLeast(0),
            progressPercent = progress
        )
    }

    private fun buildWeeklyTimeline(
        today: LocalDate,
        countsByWeekStart: Map<LocalDate, Int>,
        target: Int,
        periods: Int = 4,
    ): List<TrainingStreakProgressEntry> {
        if (target <= 0) return emptyList()
        val daysFromMonday = today.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber
        val startOfCurrentWeek = today.minus(DatePeriod(days = daysFromMonday))
        val start = startOfCurrentWeek.minus(DatePeriod(days = (periods - 1) * 7))
        return (0 until periods).map { offset ->
            val weekStart = start.plus(DatePeriod(days = offset * 7))
            val sessions = countsByWeekStart[weekStart] ?: 0
            val percent = ((sessions.toFloat() / target).coerceIn(0f, 1f) * 100).roundToInt()
            TrainingStreakProgressEntry(
                progressPercent = percent,
                achievedSessions = sessions,
                targetSessions = target
            )
        }
    }

    private fun buildDailyTimeline(
        today: LocalDate,
        dayCounts: Map<LocalDate, Int>,
        days: Int = 7,
    ): List<TrainingStreakProgressEntry> {
        if (days <= 0) return emptyList()
        return (days - 1 downTo 0).map { offset ->
            val date = today.minus(DatePeriod(days = offset))
            val sessions = dayCounts[date] ?: 0
            TrainingStreakProgressEntry(
                progressPercent = if (sessions > 0) 100 else 0,
                achievedSessions = sessions,
                targetSessions = 1
            )
        }
    }

    private data class PatternCandidate(
        val periodDays: Int,
        val shift: Int,
        val mask: List<Boolean>,
        val expectedTrainingDays: Int,
        val confidence: Float,
    )

    private fun detectBestPattern(
        series: List<Boolean>,
        maxPeriodDays: Int,
        minCycles: Int,
    ): PatternCandidate? {
        if (series.isEmpty()) return null

        val horizon = series.size
        val candidates = buildList {
            for (period in 2..maxPeriodDays) {
                if (horizon < period * minCycles) continue
                for (shift in 0 until period) {
                    val obs = IntArray(period)
                    val trains = IntArray(period)

                    for (i in 0 until horizon) {
                        val pos = (i + shift) % period
                        obs[pos]++
                        if (series[i]) trains[pos]++
                    }

                    val probs = (0 until period).map { p ->
                        if (obs[p] == 0) 0f else trains[p].toFloat() / obs[p].toFloat()
                    }

                    val mask = probs.map { it > 0.5f }
                    val expected = mask.count { it }
                    if (expected == 0 || expected == period) continue

                    val matches = (0 until horizon).count { i ->
                        val expectedTrain = mask[(i + shift) % period]
                        expectedTrain == series[i]
                    }

                    val accuracy = matches.toFloat() / horizon.toFloat()
                    val stability = probs.map { abs(it - 0.5f) * 2f }.average().toFloat()
                    val coverage =
                        (horizon.toFloat() / (period * minCycles).toFloat()).coerceIn(0f, 1f)

                    val confidence = (accuracy * stability * coverage).coerceIn(0f, 1f)

                    add(
                        PatternCandidate(
                            periodDays = period,
                            shift = shift,
                            mask = mask,
                            expectedTrainingDays = expected,
                            confidence = confidence,
                        )
                    )
                }
            }
        }

        return candidates.maxWithOrNull(
            compareBy<PatternCandidate> { it.confidence }.thenBy { it.periodDays }
        )
    }

    private suspend fun resolveExperience(): ExperienceEnum? {
        return userFeature.observeUser()
            .mapNotNull { it?.experience }
            .firstOrNull()
    }

    private fun List<WeekBucket>.deriveWeeklyTarget(): Int {
        if (isEmpty()) return 0
        val positiveCounts = map { it.count }.filter { it > 0 }
        if (positiveCounts.isEmpty()) return 0

        val recent = positiveCounts.takeLast(RECENT_WEEKS_FOR_TARGET)

        if (recent.size == 1) {
            return recent.first().coerceIn(1, 10)
        }
        if (recent.size == 2) {
            return ((recent[0] + recent[1]) / 2f).roundToInt().coerceIn(1, 10)
        }

        val sorted = recent.sorted()
        val median = sorted[sorted.size / 2]

        val mode = recent.groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val mean = recent.average().roundToInt().coerceAtLeast(1)
        val lastWeek = recent.last()
        val blended = ((median + lastWeek) / 2.0).roundToInt().coerceAtLeast(1)

        val stableCandidate = if (recent.size >= MIN_WEEKS_FOR_HISTORY_TARGET) {
            mode ?: median
        } else {
            blended
        }

        return (stableCandidate ?: mean).coerceIn(1, 10)
    }

    private data class TrainingStreakAdaptation(
        val patternHorizonDays: Int,
        val patternMinCycles: Int,
        val patternMinConfidence: Float,
        val rhythmMinBlocks: Int,
        val rhythmMinConfidence: Float,
    )

    private fun ExperienceEnum.trainingStreakAdaptation(): TrainingStreakAdaptation {
        return when (this) {
            ExperienceEnum.BEGINNER -> TrainingStreakAdaptation(
                patternHorizonDays = 42,
                patternMinCycles = 2,
                patternMinConfidence = 0.5f,
                rhythmMinBlocks = 2,
                rhythmMinConfidence = 0.45f,
            )

            ExperienceEnum.INTERMEDIATE -> TrainingStreakAdaptation(
                patternHorizonDays = 56,
                patternMinCycles = 3,
                patternMinConfidence = 0.55f,
                rhythmMinBlocks = 3,
                rhythmMinConfidence = 0.5f,
            )

            ExperienceEnum.ADVANCED -> TrainingStreakAdaptation(
                patternHorizonDays = 63,
                patternMinCycles = 3,
                patternMinConfidence = 0.6f,
                rhythmMinBlocks = 3,
                rhythmMinConfidence = 0.55f,
            )

            ExperienceEnum.PRO -> TrainingStreakAdaptation(
                patternHorizonDays = 70,
                patternMinCycles = 4,
                patternMinConfidence = 0.65f,
                rhythmMinBlocks = 4,
                rhythmMinConfidence = 0.6f,
            )
        }
    }
}
