package com.grippo.domain.state.training.transformation

import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.highlight.Highlight
import com.grippo.core.state.trainings.highlight.HighlightMuscleFocus
import com.grippo.core.state.trainings.highlight.HighlightMuscleFocusSegment
import com.grippo.core.state.trainings.highlight.HighlightPerformanceMetric
import com.grippo.core.state.trainings.highlight.HighlightPerformanceStatus
import com.grippo.core.state.trainings.highlight.HighlightStreak
import com.grippo.core.state.trainings.highlight.HighlightStreakFeatured
import com.grippo.core.state.trainings.highlight.HighlightStreakMood
import com.grippo.core.state.trainings.highlight.HighlightStreakProgressEntry
import com.grippo.core.state.trainings.highlight.HighlightStreakRhythm
import com.grippo.core.state.trainings.highlight.HighlightStreakType
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.logger.AppLogger
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes

public fun List<TrainingState>.toHighlight(
    exerciseExamples: List<ExerciseExampleState> = emptyList(),
): Highlight {
    val trainings = this
    val totalDuration: Duration = trainings.fold(ZERO) { acc, training -> acc + training.duration }
    val exampleIndex = exerciseExamples.associateBy { it.value.id }

    return Highlight(
        totalDuration = totalDuration,
        focusExercise = trainings.focusExercise(exampleIndex),
        muscleFocus = trainings.muscleFocus(exampleIndex),
        streak = trainings.streak(),
        performance = trainings.performanceMetrics()
    )
}

private fun List<TrainingState>.focusExercise(
    exerciseExamples: Map<String, ExerciseExampleState>,
): ExerciseExampleState? {
    if (isEmpty()) {
        AppLogger.General.warning("Highlights: focus exercise skipped (no trainings)")
        return null
    }

    if (exerciseExamples.isEmpty()) {
        AppLogger.General.warning("Highlights: focus exercise skipped (exercise examples cache empty)")
        return null
    }

    val volumes = flatMap { training ->
        training.exercises.mapNotNull { exercise ->
            val volume = exercise.metrics.volume.value ?: return@mapNotNull null
            ExerciseVolume(exercise, volume)
        }
    }
    if (volumes.isEmpty()) {
        AppLogger.General.warning("Highlights: focus exercise skipped (no exercises with volume)")
        return null
    }

    val topEntry = volumes
        .groupBy { it.exercise.exerciseExample.id }
        .maxByOrNull { entry -> entry.value.sumOf { item -> item.volume.toDouble() } }
        ?: run {
            AppLogger.General.warning("Highlights: focus exercise skipped (unable to determine top exercise)")
            return null
        }

    val sample = topEntry.value.first().exercise
    val totalVolume = topEntry.value.sumOf { it.volume.toDouble() }.toFloat()
    if (totalVolume <= 0f) {
        AppLogger.General.warning(
            "Highlights: focus exercise skipped (non-positive total volume) exampleId=${sample.exerciseExample.id}"
        )
        return null
    }

    val example = exerciseExamples[sample.exerciseExample.id]
    if (example == null) {
        AppLogger.General.warning(
            "Highlights: focus exercise example missing exampleId=${sample.exerciseExample.id} cacheSize=${exerciseExamples.size}"
        )
        return null
    }

    AppLogger.General.warning(
        "Highlights: focus exercise selected id=${example.value.id} sessions=${topEntry.value.size} totalVolume=$totalVolume"
    )
    return example
}

private fun List<TrainingState>.muscleFocus(
    exerciseExamples: Map<String, ExerciseExampleState>,
): HighlightMuscleFocus? {
    if (isEmpty()) return null

    val contributions = mutableMapOf<MuscleGroupEnumState, Float>()

    for (exercise in flatMap { it.exercises }) {
        val volume = exercise.metrics.volume.value?.takeIf { it > 0f } ?: continue
        val exampleId = exercise.exerciseExample.id
        val example = exerciseExamples[exampleId]
        if (example != null) {
            val bundles = example.bundles.mapNotNull { bundle ->
                val sharePercent = when (val format = bundle.percentage) {
                    is PercentageFormatState.Valid -> format.value.toFloat()
                    is PercentageFormatState.Invalid -> format.value?.toFloat() ?: 0f
                    is PercentageFormatState.Empty -> 0f
                }
                if (sharePercent <= 0f) {
                    null
                } else {
                    val group = when (bundle.muscle.type) {
                        MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
                        MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL,
                        MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL -> MuscleGroupEnumState.CHEST_MUSCLES

                        MuscleEnumState.TRAPEZIUS,
                        MuscleEnumState.LATISSIMUS_DORSI,
                        MuscleEnumState.RHOMBOIDS,
                        MuscleEnumState.TERES_MAJOR -> MuscleGroupEnumState.BACK_MUSCLES

                        MuscleEnumState.RECTUS_ABDOMINIS,
                        MuscleEnumState.OBLIQUES -> MuscleGroupEnumState.ABDOMINAL_MUSCLES

                        MuscleEnumState.CALF,
                        MuscleEnumState.GLUTEAL,
                        MuscleEnumState.HAMSTRINGS,
                        MuscleEnumState.QUADRICEPS,
                        MuscleEnumState.ADDUCTORS,
                        MuscleEnumState.ABDUCTORS -> MuscleGroupEnumState.LEGS

                        MuscleEnumState.ANTERIOR_DELTOID,
                        MuscleEnumState.LATERAL_DELTOID,
                        MuscleEnumState.POSTERIOR_DELTOID -> MuscleGroupEnumState.SHOULDER_MUSCLES

                        MuscleEnumState.BICEPS,
                        MuscleEnumState.TRICEPS,
                        MuscleEnumState.FOREARM -> MuscleGroupEnumState.ARMS_AND_FOREARMS
                    }
                    group to sharePercent
                }
            }

            val totalShare = bundles.fold(0f) { acc, entry -> acc + entry.second }
            if (bundles.isNotEmpty() && totalShare > 0f) {
                bundles.forEach { (group, sharePercent) ->
                    val share = (sharePercent / totalShare) * volume
                    contributions[group] = (contributions[group] ?: 0f) + share
                }
                continue
            }
        }

        val fallbackGroup = when (exercise.exerciseExample.forceType) {
            ForceTypeEnumState.PULL -> MuscleGroupEnumState.BACK_MUSCLES
            ForceTypeEnumState.PUSH -> MuscleGroupEnumState.CHEST_MUSCLES
            ForceTypeEnumState.HINGE -> MuscleGroupEnumState.LEGS
        }

        contributions[fallbackGroup] = (contributions[fallbackGroup] ?: 0f) + volume
    }

    val totalLoad = contributions.values.sum()
    if (totalLoad <= 0f) return null

    val segments = contributions
        .mapNotNull { (group, loadValue) ->
            if (loadValue <= 0f) {
                null
            } else {
                val percentage = ((loadValue / totalLoad) * 100).roundToInt()
                if (percentage <= 0) {
                    null
                } else {
                    HighlightMuscleFocusSegment(
                        muscleGroup = group,
                        load = PercentageFormatState.of(percentage)
                    )
                }
            }
        }
        .sortedByDescending { segment ->
            when (val state = segment.load) {
                is PercentageFormatState.Valid -> state.value
                is PercentageFormatState.Invalid -> state.value ?: 0
                is PercentageFormatState.Empty -> 0
            }
        }

    if (segments.isEmpty()) return null

    return HighlightMuscleFocus(
        segments = segments
    )
}

private fun List<TrainingState>.streak(): HighlightStreak {
    val today = DateTimeUtils.now().date

    if (isEmpty()) {
        return emptyStreak(today)
    }

    val sessionsByDay = groupingBy { it.createdAt.date }.eachCount()
    if (sessionsByDay.isEmpty()) {
        return emptyStreak(today)
    }

    val weekBuckets = buildWeekBuckets()
    val weeklyData = weekBuckets.weeklyData(today)
    val dailyData = sessionsByDay.dailyData(today)
    val dayBlocks = sessionsByDay.keys.sorted().toDayBlocks()
    val rhythmPattern = detectRhythmPattern(dayBlocks)

    val shouldFeatureWeekly = weeklyData.targetSessions > 0 &&
            (weeklyData.streakLength > 0 ||
                    weekBuckets.size >= 3 ||
                    dailyData.currentStreak < 3)

    val computation = when {
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

    return HighlightStreak(
        totalActiveDays = sessionsByDay.size,
        featured = computation.featured,
        timeline = computation.timeline
    )
}

private fun emptyStreak(today: LocalDate): HighlightStreak {
    return HighlightStreak(
        totalActiveDays = 0,
        featured = HighlightStreakFeatured(
            type = HighlightStreakType.Daily,
            length = 0,
            targetSessionsPerPeriod = 1,
            periodLengthDays = 1,
            mood = HighlightStreakMood.Restart,
            progressPercent = 0
        ),
        timeline = buildDailyTimeline(today, emptyMap())
    )
}

private data class StreakComputation(
    val featured: HighlightStreakFeatured,
    val timeline: List<HighlightStreakProgressEntry>,
)

private fun computeWeeklyStreak(
    weeklyData: WeeklyStreakData,
    today: LocalDate,
): StreakComputation {
    val featured = HighlightStreakFeatured(
        type = HighlightStreakType.Weekly,
        length = weeklyData.streakLength,
        targetSessionsPerPeriod = weeklyData.targetSessions,
        periodLengthDays = 7,
        mood = determineStreakMood(weeklyData.streakLength),
        progressPercent = weeklyData.progressPercent
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
    val featured = HighlightStreakFeatured(
        type = HighlightStreakType.Daily,
        length = dailyData.currentStreak,
        targetSessionsPerPeriod = 1,
        periodLengthDays = 1,
        mood = determineStreakMood(dailyData.currentStreak),
        progressPercent = dailyData.progressPercent
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
            featured = HighlightStreakFeatured(
                type = HighlightStreakType.Rhythm,
                length = 0,
                targetSessionsPerPeriod = pattern.workDays,
                periodLengthDays = pattern.workDays + pattern.restDays,
                mood = HighlightStreakMood.Restart,
                progressPercent = 0,
                rhythm = HighlightStreakRhythm(
                    workDays = pattern.workDays,
                    restDays = pattern.restDays
                )
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

    val featured = HighlightStreakFeatured(
        type = HighlightStreakType.Rhythm,
        length = streak,
        targetSessionsPerPeriod = pattern.workDays,
        periodLengthDays = pattern.workDays + pattern.restDays,
        mood = determineStreakMood(streak),
        progressPercent = blockProgress,
        rhythm = HighlightStreakRhythm(
            workDays = pattern.workDays,
            restDays = pattern.restDays
        )
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
)

private data class DayBlock(
    val start: LocalDate,
    val length: Int,
) {
    val end: LocalDate = start.plusDays(length - 1)
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
        if (current == previous.plusDays()) {
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

    return RhythmPattern(workDays = workDays, restDays = restDays)
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
): List<HighlightStreakProgressEntry> {
    if (blocks.isEmpty()) return emptyList()
    val recent = blocks.takeLast(periods)
    val entries = recent.map { block ->
        val achieved = block.length.coerceAtMost(pattern.workDays)
        val percent = ((achieved.toFloat() / pattern.workDays).coerceIn(0f, 1f) * 100).roundToInt()
        HighlightStreakProgressEntry(
            progressPercent = percent,
            achievedSessions = achieved,
            targetSessions = pattern.workDays
        )
    }
    if (entries.size >= periods) return entries
    val padding = List(periods - entries.size) {
        HighlightStreakProgressEntry(
            progressPercent = 0,
            achievedSessions = 0,
            targetSessions = pattern.workDays
        )
    }
    return padding + entries
}

private const val RHYTHM_MIN_BLOCKS = 3
private const val RHYTHM_MIN_CONFIDENCE = 0.5f
private const val RHYTHM_TOLERANCE_DAYS = 1
private const val RHYTHM_MAX_WORK_DAYS = 5
private const val RHYTHM_MAX_REST_DAYS = 4
private const val RHYTHM_MAX_CYCLE_LENGTH = 8

private data class WeekBucket(
    val start: LocalDate,
    val count: Int,
)

private fun List<TrainingState>.buildWeekBuckets(): List<WeekBucket> {
    if (isEmpty()) return emptyList()
    val buckets = mutableMapOf<LocalDate, Int>()
    for (training in this) {
        val weekStart = training.createdAt.date.startOfWeek()
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
        (mode ?: average).coerceIn(1, 6)
    }

    val currentWeekStart = today.startOfWeek()
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
        if (firstIteration && pointer == currentWeekStart && (countsByWeekStart[pointer]
                ?: 0) < target
        ) {
            pointer = pointer.minusWeeks()
            firstIteration = false
            continue
        }
        firstIteration = false
        val value = countsByWeekStart[pointer] ?: 0
        if (value >= target) {
            streak++
            pointer = pointer.minusWeeks()
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
            if (candidate == lastDate.minusDays()) {
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

private fun determineStreakMood(length: Int): HighlightStreakMood {
    return when {
        length >= 4 -> HighlightStreakMood.CrushingIt
        length >= 2 -> HighlightStreakMood.OnTrack
        else -> HighlightStreakMood.Restart
    }
}

private fun buildWeeklyTimeline(
    today: LocalDate,
    countsByWeekStart: Map<LocalDate, Int>,
    target: Int,
    periods: Int = 4,
): List<HighlightStreakProgressEntry> {
    if (target <= 0) return emptyList()
    val start = today.startOfWeek().minusWeeks(periods - 1)
    return (0 until periods).map { offset ->
        val weekStart = start.plusWeeks(offset)
        val sessions = countsByWeekStart[weekStart] ?: 0
        val percent = ((sessions.toFloat() / target).coerceIn(0f, 1f) * 100).roundToInt()
        HighlightStreakProgressEntry(
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
): List<HighlightStreakProgressEntry> {
    if (days <= 0) return emptyList()
    return (days - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset)
        val sessions = dayCounts[date] ?: 0
        HighlightStreakProgressEntry(
            progressPercent = if (sessions > 0) 100 else 0,
            achievedSessions = sessions,
            targetSessions = 1
        )
    }
}

private fun LocalDate.startOfWeek(): LocalDate {
    val daysFromMonday = dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber
    return minus(DatePeriod(days = daysFromMonday))
}

private fun LocalDate.minusWeeks(weeks: Int = 1): LocalDate {
    return minus(DatePeriod(days = weeks * 7))
}

private fun LocalDate.plusWeeks(weeks: Int = 1): LocalDate {
    return plus(DatePeriod(days = weeks * 7))
}

private fun LocalDate.minusDays(days: Int = 1): LocalDate {
    return minus(DatePeriod(days = days))
}

private fun LocalDate.plusDays(days: Int = 1): LocalDate {
    return plus(DatePeriod(days = days))
}

private fun List<TrainingState>.performanceMetrics(): List<HighlightPerformanceMetric> {
    val latestTraining = maxByOrNull { it.createdAt } ?: return emptyList()
    val performance = mutableListOf<HighlightPerformanceMetric>()

    // 1) Duration (vs average)
    run {
        val values = map { it.duration.inWholeMinutes.toDouble() }
        val current = latestTraining.duration.inWholeMinutes.toDouble()
        if (values.isNotEmpty() && current > 0) {
            val average = values.average()
            val best = values.maxOrNull() ?: current
            val delta = percentageDelta(current, average) ?: 0
            val status = determinePerformanceStatus(delta = delta, current = current, best = best)
            performance += HighlightPerformanceMetric.Duration(
                deltaPercentage = delta,
                current = latestTraining.duration,
                average = average.minutes,
                best = best.minutes,
                status = status
            )
        }
    }

    // 2) Volume (vs average)
    run {
        val currentValue = latestTraining.metrics.volume.value
        val values = mapNotNull { it.metrics.volume.value?.toDouble() }
        if (currentValue != null && values.isNotEmpty()) {
            val average = values.average()
            val best = values.maxOrNull() ?: currentValue.toDouble()
            val delta = percentageDelta(currentValue.toDouble(), average) ?: 0
            val status = determinePerformanceStatus(
                delta = delta,
                current = currentValue.toDouble(),
                best = best
            )
            performance += HighlightPerformanceMetric.Volume(
                deltaPercentage = delta,
                current = latestTraining.metrics.volume,
                average = VolumeFormatState.of(average.toFloat()),
                best = VolumeFormatState.of(best.toFloat()),
                status = status
            )
        }
    }

    // 3) Repetitions (vs average)
    run {
        val currentValue = latestTraining.metrics.repetitions.value
        val values = mapNotNull { it.metrics.repetitions.value?.toDouble() }
        if (currentValue == null || values.isEmpty()) {
            val nonNullCount = count { it.metrics.repetitions.value != null }
            AppLogger.General.warning(
                "Highlights: repetitions trend skipped (no reps value). " +
                        "latestRepetitions=${latestTraining.metrics.repetitions} " +
                        "nonNullTrainings=$nonNullCount totalTrainings=${size}"
            )
            return@run
        }

        val average = values.average()
        val best = values.maxOrNull() ?: currentValue.toDouble()
        val delta = percentageDelta(currentValue.toDouble(), average) ?: 0
        val status = determinePerformanceStatus(
            delta = delta,
            current = currentValue.toDouble(),
            best = best
        )
        performance += HighlightPerformanceMetric.Repetitions(
            deltaPercentage = delta,
            current = latestTraining.metrics.repetitions,
            average = RepetitionsFormatState.of(average.toInt()),
            best = RepetitionsFormatState.of(best.toInt()),
            status = status
        )
    }

    // 4) Intensity (vs average)
    run {
        val currentValue = latestTraining.metrics.intensity.value
        val values = mapNotNull { it.metrics.intensity.value?.toDouble() }
        if (currentValue != null && values.isNotEmpty()) {
            val average = values.average()
            val best = values.maxOrNull() ?: currentValue.toDouble()
            val delta = percentageDelta(currentValue.toDouble(), average) ?: 0
            val status = determinePerformanceStatus(
                delta = delta,
                current = currentValue.toDouble(),
                best = best
            )
            performance += HighlightPerformanceMetric.Intensity(
                deltaPercentage = delta,
                current = latestTraining.metrics.intensity,
                average = IntensityFormatState.of(average.toFloat()),
                best = IntensityFormatState.of(best.toFloat()),
                status = status
            )
        }
    }

    return performance
}

private fun percentageDelta(current: Double, average: Double): Int? {
    if (average == 0.0) return null
    val delta = ((current - average) / average) * 100
    return delta.roundToInt()
}

private const val PERFORMANCE_EPS: Double = 1e-2
private const val IMPROVED_THRESHOLD: Int = 5
private const val DECLINED_THRESHOLD: Int = -5

private fun determinePerformanceStatus(
    delta: Int,
    current: Double,
    best: Double?,
): HighlightPerformanceStatus {
    val bestValue = best ?: current
    if (current >= bestValue - PERFORMANCE_EPS) {
        return HighlightPerformanceStatus.Record
    }
    return when {
        delta >= IMPROVED_THRESHOLD -> HighlightPerformanceStatus.Improved
        delta <= DECLINED_THRESHOLD -> HighlightPerformanceStatus.Declined
        else -> HighlightPerformanceStatus.Stable
    }
}

private data class ExerciseVolume(
    val exercise: ExerciseState,
    val volume: Float,
)
