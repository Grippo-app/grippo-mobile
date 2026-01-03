package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.metrics.models.TrainingStreak
import com.grippo.data.features.api.metrics.models.TrainingStreakFeatured
import com.grippo.data.features.api.metrics.models.TrainingStreakMood
import com.grippo.data.features.api.metrics.models.TrainingStreakProgressEntry
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.roundToInt

public class TrainingStreakUseCase {

    private companion object {
        private const val RHYTHM_MIN_BLOCKS = 3
        private const val RHYTHM_MIN_CONFIDENCE = 0.5f
        private const val RHYTHM_TOLERANCE_DAYS = 1
        private const val RHYTHM_MAX_WORK_DAYS = 5
        private const val RHYTHM_MAX_REST_DAYS = 4
        private const val RHYTHM_MAX_CYCLE_LENGTH = 8
    }

    public fun fromTrainings(trainings: List<Training>): TrainingStreak {
        val today = DateTimeUtils.now().date
        if (trainings.isEmpty()) {
            return emptyStreak(today)
        }

        val sessionsByDay = trainings.groupingBy { it.createdAt.date }.eachCount()
        if (sessionsByDay.isEmpty()) {
            return emptyStreak(today)
        }

        val weekBuckets = trainings.buildWeekBuckets()
        val weeklyData = weekBuckets.weeklyData(today)
        val dailyData = sessionsByDay.dailyData(today)
        val dayBlocks = sessionsByDay.keys.sorted().toDayBlocks()
        val rhythmPattern = detectRhythmPattern(dayBlocks)

        val patternComputation = computePatternStreak(
            sessionsByDay = sessionsByDay,
            today = today,
        )

        val shouldFeatureWeekly = weeklyData.targetSessions > 0 &&
                (weeklyData.streakLength > 0 ||
                        weekBuckets.size >= 3 ||
                        dailyData.currentStreak < 3)

        val computation = when {
            patternComputation != null -> patternComputation
            rhythmPattern != null -> computeRhythmStreak(
                pattern = rhythmPattern,
                blocks = dayBlocks
            )

            shouldFeatureWeekly -> computeWeeklyStreak(
                weeklyData = weeklyData,
                today = today
            )

            else -> computeDailyStreak(
                dailyData = dailyData,
                today = today,
                sessionsByDay = sessionsByDay
            )
        }

        return TrainingStreak(
            totalActiveDays = sessionsByDay.size,
            featured = computation.featured,
            timeline = computation.timeline
        )
    }

    private fun emptyStreak(today: LocalDate): TrainingStreak {
        return TrainingStreak(
            totalActiveDays = 0,
            featured = TrainingStreakFeatured.Daily(
                length = 0,
                mood = TrainingStreakMood.Restart,
                progressPercent = 0,
                confidence = 0f,
            ),
            timeline = buildDailyTimeline(today, emptyMap())
        )
    }

    private data class StreakComputation(
        val featured: TrainingStreakFeatured,
        val timeline: List<TrainingStreakProgressEntry>,
    )

    private fun computeWeeklyStreak(
        weeklyData: WeeklyStreakData,
        today: LocalDate,
    ): StreakComputation {
        val confidence = (weeklyData.countsByWeekStart.size.toFloat() / 4f).coerceIn(0f, 1f)
        val featured = TrainingStreakFeatured.Weekly(
            length = weeklyData.streakLength,
            targetSessionsPerWeek = weeklyData.targetSessions,
            mood = determineStreakMood(weeklyData.streakLength),
            progressPercent = weeklyData.progressPercent,
            confidence = confidence,
        )

        val timeline = buildWeeklyTimeline(
            today = today,
            countsByWeekStart = weeklyData.countsByWeekStart,
            target = weeklyData.targetSessions
        )

        return StreakComputation(featured = featured, timeline = timeline)
    }

    private fun computeDailyStreak(
        dailyData: DailyStreakData,
        today: LocalDate,
        sessionsByDay: Map<LocalDate, Int>,
    ): StreakComputation {
        val confidence = (sessionsByDay.size.toFloat() / 7f).coerceIn(0f, 1f)
        val featured = TrainingStreakFeatured.Daily(
            length = dailyData.currentStreak,
            mood = determineStreakMood(dailyData.currentStreak),
            progressPercent = dailyData.progressPercent,
            confidence = confidence,
        )

        val timeline = buildDailyTimeline(
            today = today,
            dayCounts = sessionsByDay
        )

        return StreakComputation(featured = featured, timeline = timeline)
    }

    private fun computeRhythmStreak(
        pattern: RhythmPattern,
        blocks: List<DayBlock>,
    ): StreakComputation {
        if (blocks.isEmpty()) {
            return StreakComputation(
                featured = TrainingStreakFeatured.Rhythm(
                    length = 0,
                    workDays = pattern.workDays,
                    restDays = pattern.restDays,
                    mood = TrainingStreakMood.Restart,
                    progressPercent = 0,
                    confidence = 0f,
                ),
                timeline = emptyList()
            )
        }

        val latestBlock = blocks.last()
        val blockProgress = ((latestBlock.length.coerceAtMost(pattern.workDays)
            .toFloat() / pattern.workDays) * 100).roundToInt()

        var pointer = blocks.lastIndex
        if (blocks[pointer].length < pattern.workDays) {
            pointer--
        }

        var streak = 0
        var index = pointer
        while (index >= 0) {
            val block = blocks[index]
            if (block.length < pattern.workDays) {
                break
            }
            streak++
            val previousIndex = index - 1
            if (previousIndex < 0) {
                break
            }
            val restGap = restBetween(blocks[previousIndex], block)
            if (restGap <= pattern.restDays + RHYTHM_TOLERANCE_DAYS) {
                index--
            } else {
                break
            }
        }

        val featured = TrainingStreakFeatured.Rhythm(
            length = streak,
            workDays = pattern.workDays,
            restDays = pattern.restDays,
            mood = determineStreakMood(streak),
            progressPercent = blockProgress,
            confidence = pattern.confidence,
        )

        val timeline = buildRhythmTimeline(
            blocks = blocks,
            pattern = pattern
        )

        return StreakComputation(featured = featured, timeline = timeline)
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

    private fun detectRhythmPattern(blocks: List<DayBlock>): RhythmPattern? {
        if (blocks.size < RHYTHM_MIN_BLOCKS) return null
        val runLengths = blocks.map { it.length }
        val restLengths = blocks.restLengths()
        if (restLengths.size < RHYTHM_MIN_BLOCKS - 1) return null

        val runMode = runLengths.modeOrNull() ?: return null
        val restMode = restLengths.modeOrNull() ?: return null

        if (runMode.value < 2 || restMode.value <= 0) return null

        val runConfidence = runMode.count.toFloat() / runLengths.size
        val restConfidence = restMode.count.toFloat() / restLengths.size
        val multiDayFraction = runLengths.count { it >= 2 }.toFloat() / runLengths.size

        if (runConfidence < RHYTHM_MIN_CONFIDENCE ||
            restConfidence < RHYTHM_MIN_CONFIDENCE ||
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
            if (rest > 0) {
                rests += rest
            }
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
        val positiveCounts = map { it.count }.filter { it > 0 }
        val weeklyTarget = if (positiveCounts.isEmpty()) {
            0
        } else {
            val mode = positiveCounts
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { (_, occurrences) -> occurrences }
                ?.key
            val average = positiveCounts.average().roundToInt().coerceAtLeast(1)
            val target = mode ?: average
            target.coerceIn(1, 6)
        }

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
        if (isEmpty()) {
            return DailyStreakData(currentStreak = 0, progressPercent = 0)
        }

        val sortedDates = keys.sorted()
        var streak = 1
        if (sortedDates.isNotEmpty()) {
            streak = 1
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
            if (gapFromToday > 1) {
                streak = 0
            }
        }

        val todaySessions = this[today] ?: 0
        val progress = if (todaySessions > 0) 100 else 0
        return DailyStreakData(
            currentStreak = streak.coerceAtLeast(0),
            progressPercent = progress
        )
    }

    private fun determineStreakMood(length: Int): TrainingStreakMood {
        return when {
            length >= 4 -> TrainingStreakMood.CrushingIt
            length >= 2 -> TrainingStreakMood.OnTrack
            else -> TrainingStreakMood.Restart
        }
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

    private fun computePatternStreak(
        sessionsByDay: Map<LocalDate, Int>,
        today: LocalDate,
        horizonDays: Int = 56,
        maxPeriodDays: Int = 28,
        minCycles: Int = 3,
        minConfidence: Float = 0.6f,
    ): StreakComputation? {
        if (sessionsByDay.isEmpty()) return null

        val series = (0 until horizonDays).map { offset ->
            val date = today.minus(DatePeriod(days = offset))
            val trained = (sessionsByDay[date] ?: 0) > 0
            trained
        }
        // Need enough history to support at least minCycles of some period.
        if (series.count { it } < 3) return null

        val candidate = detectBestPattern(
            series = series,
            maxPeriodDays = maxPeriodDays,
            minCycles = minCycles,
        ) ?: return null

        if (candidate.confidence < minConfidence) return null

        val target = candidate.expectedTrainingDays
        if (target <= 0) return null

        // Cycle index: 0 is current cycle (may be partial), increasing into the past.
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

        val featured = TrainingStreakFeatured.Pattern(
            length = streakCycles,
            targetSessionsPerPeriod = target,
            periodLengthDays = candidate.periodDays,
            mask = candidate.mask,
            mood = determineStreakMood(streakCycles),
            progressPercent = currentProgress,
            confidence = candidate.confidence,
        )

        return StreakComputation(
            featured = featured,
            timeline = timeline,
        )
    }

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
                    val stability =
                        probs.map { kotlin.math.abs(it - 0.5f) * 2f }.average().toFloat()
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

        return candidates
            .maxWithOrNull(compareBy<PatternCandidate> { it.confidence }
                .thenBy { it.periodDays })
    }
}
