package com.grippo.data.features.api.metrics.engagement

import com.grippo.data.features.api.metrics.engagement.models.TrainingStreak
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreakFeatured
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreakMood
import com.grippo.data.features.api.metrics.engagement.models.TrainingStreakProgressEntry
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.roundToInt

public class TrainingStreakUseCase {

    private companion object {
        private const val MIN_COMPLETED_WEEKS = 3
        private const val TARGET_HISTORY_WEEKS = 8
        private const val MIN_COMPLETED_CYCLES = 3
        private const val PATTERN_HISTORY_CYCLES = 8
        private const val MAX_PATTERN_DAYS = 28
        private const val MIN_PATTERN_TRAINING_DAYS = 6
        private const val MIN_PATTERN_AGREEMENT = 0.8f
        private const val MAX_REST_TRAINING_PROPORTION = 0.2f
        private const val DAILY_HISTORY_DAYS = 14
        private const val DAILY_DENSITY_THRESHOLD = 0.85f
    }

    /** The supplied range is the loaded history window; absent dates inside it are inactive dates. */
    public fun fromTrainings(
        trainings: List<Training>,
        range: DateRange,
        now: LocalDateTime = DateTimeUtils.now(),
    ): TrainingStreak {
        require(range.from <= range.to)

        val evaluatedAt = minOf(range.to, now)
        val activeDates = trainings
            .filter { training ->
                training.createdAt >= range.from && training.createdAt <= evaluatedAt &&
                        training.repetitions > 0
            }
            .groupBy(Training::id)
            .values
            .map { versions -> versions.maxBy(Training::createdAt).createdAt.date }
            .toSet()

        val firstTrainingDate = activeDates.minOrNull()
        if (firstTrainingDate == null) {
            return TrainingStreak(
                totalActiveDays = 0,
                featured = TrainingStreakFeatured.Daily(
                    length = 0,
                    mood = TrainingStreakMood.Restart,
                    progressPercent = 0,
                    confidence = 0f,
                ),
                timeline = emptyList(),
                historyDays = 0,
                lastTrainingGapDays = -1,
            )
        }

        val observation = Observation(
            activeDates = activeDates,
            from = maxOf(range.from.date, firstTrainingDate),
            date = evaluatedAt.date,
            isCurrentDayOpen = evaluatedAt.date == now.date,
        )
        val daily = dailyCandidate(observation)
        val weekly = weeklyCandidate(observation)
        val cyclic = cyclicCandidate(observation)

        val dailyDensity = recentDailyDensity(observation)
        // A supported cycle describes train/rest timing beyond a weekly count.
        // Their evidence measures differ, so comparing them as probabilities would mislead.
        val featured = when {
            daily.featured.length >= 3 && dailyDensity >= DAILY_DENSITY_THRESHOLD -> daily
            cyclic != null -> cyclic
            weekly != null -> weekly
            else -> daily
        }

        return TrainingStreak(
            totalActiveDays = activeDates.size,
            featured = featured.featured,
            timeline = featured.timeline,
            historyDays = observation.historyDays,
            lastTrainingGapDays = observation.lastTrainingGapDays,
        )
    }

    private data class Observation(
        val activeDates: Set<LocalDate>,
        val from: LocalDate,
        val date: LocalDate,
        val isCurrentDayOpen: Boolean,
    ) {
        val historyDays: Int = from.daysUntil(date) + 1
        val lastTrainingGapDays: Int = activeDates.max().daysUntil(date)
        val completedThrough: LocalDate = if (isCurrentDayOpen) {
            date.minus(DatePeriod(days = 1))
        } else {
            date
        }
    }

    private data class Candidate(
        val featured: TrainingStreakFeatured,
        val timeline: List<TrainingStreakProgressEntry>,
    )

    private fun dailyCandidate(observation: Observation): Candidate {
        var pointer = if (observation.date in observation.activeDates) {
            observation.date
        } else if (observation.isCurrentDayOpen) {
            observation.date.minus(DatePeriod(days = 1))
        } else {
            observation.date
        }
        var length = 0
        while (pointer in observation.activeDates) {
            length++
            pointer = pointer.minus(DatePeriod(days = 1))
        }

        val progress = if (observation.date in observation.activeDates) {
            100
        } else {
            0
        }
        val mood = when {
            length >= 4 -> TrainingStreakMood.CrushingIt
            length > 0 -> TrainingStreakMood.OnTrack
            observation.lastTrainingGapDays >= 14 -> TrainingStreakMood.Restart
            else -> TrainingStreakMood.Paused
        }
        val density = recentDailyDensity(observation)
        val completedDays = observation.from.daysUntil(observation.completedThrough) + 1
        val confidence = density * (completedDays.toFloat() / DAILY_HISTORY_DAYS).coerceIn(0f, 1f)
        val timelineStart = maxOf(
            observation.from,
            observation.date.minus(DatePeriod(days = 6)),
        )
        val timeline = (0..timelineStart.daysUntil(observation.date)).map { offset ->
            val date = timelineStart.plus(DatePeriod(days = offset))
            progressEntry(
                observation = observation,
                from = date,
                to = date,
                achieved = if (date in observation.activeDates) 1 else 0,
                target = 1,
            )
        }

        return Candidate(
            featured = TrainingStreakFeatured.Daily(
                length = length,
                mood = mood,
                progressPercent = progress,
                confidence = confidence,
            ),
            timeline = timeline,
        )
    }

    private fun recentDailyDensity(observation: Observation): Float {
        val end = observation.completedThrough
        if (end < observation.from) {
            return 0f
        }

        val start = maxOf(observation.from, end.minus(DatePeriod(days = DAILY_HISTORY_DAYS - 1)))
        val activeDays = observation.activeDates.count { it in start..end }
        return activeDays.toFloat() / (start.daysUntil(end) + 1)
    }

    private fun weeklyCandidate(observation: Observation): Candidate? {
        val firstWeekStart = weekStart(observation.from)
        val firstCompleteWeek = if (firstWeekStart == observation.from) {
            firstWeekStart
        } else {
            firstWeekStart.plus(DatePeriod(days = 7))
        }
        val completedWeekStart = weekStart(observation.completedThrough)
        val lastCompleteWeek = if (
            completedWeekStart.plus(DatePeriod(days = 6)) <= observation.completedThrough
        ) {
            completedWeekStart
        } else {
            completedWeekStart.minus(DatePeriod(days = 7))
        }
        val completeWeeks = firstCompleteWeek.daysUntil(lastCompleteWeek) / 7 + 1
        if (lastCompleteWeek < firstCompleteWeek || completeWeeks < MIN_COMPLETED_WEEKS) {
            return null
        }

        val evidenceWeeks = minOf(completeWeeks, TARGET_HISTORY_WEEKS)
        val evidenceStart = lastCompleteWeek.minus(DatePeriod(days = (evidenceWeeks - 1) * 7))
        val counts = (0 until evidenceWeeks).map { index ->
            val start = evidenceStart.plus(DatePeriod(days = index * 7))
            observation.activeDates.count { it in start..start.plus(DatePeriod(days = 6)) }
        }
        val sorted = counts.sorted()
        val middle = sorted.size / 2
        val target = if (sorted.size % 2 == 0) {
            ((sorted[middle - 1] + sorted[middle]) / 2f).roundToInt()
        } else {
            sorted[middle]
        }
        if (target == 0) {
            return null
        }

        val confidence = counts.count { it >= target }.toFloat() / counts.size
        val schedule = Schedule(
            anchor = firstWeekStart,
            periodDays = 7,
            target = target,
            confidence = confidence,
        )
        val progress = periodProgress(observation = observation, schedule = schedule)
        return Candidate(
            featured = TrainingStreakFeatured.Weekly(
                length = progress.length,
                targetTrainingDaysPerWeek = target,
                mood = progress.mood,
                progressPercent = progress.percent,
                confidence = confidence,
            ),
            timeline = progress.timeline,
        )
    }

    private data class Schedule(
        val anchor: LocalDate,
        val periodDays: Int,
        val target: Int,
        val confidence: Float,
        val mask: List<Boolean> = emptyList(),
    )

    private fun cyclicCandidate(observation: Observation): Candidate? {
        val schedule = (2..MAX_PATTERN_DAYS)
            .mapNotNull { period ->
                detectSchedule(observation = observation, period = period)
            }
            .maxWithOrNull(
                compareBy<Schedule> { it.confidence }.thenBy { -it.periodDays }
            ) ?: return null

        // A seven-day recurrence belongs to the calendar-week metric.
        if (schedule.periodDays == 7) {
            return null
        }

        val progress = periodProgress(observation = observation, schedule = schedule)
        val workDays = schedule.mask.takeWhile { it }.size
        val isRhythm = workDays > 0 && schedule.mask.drop(workDays).none { it }
        val featured = if (isRhythm) {
            TrainingStreakFeatured.Rhythm(
                length = progress.length,
                workDays = workDays,
                restDays = schedule.periodDays - workDays,
                mood = progress.mood,
                progressPercent = progress.percent,
                confidence = schedule.confidence,
            )
        } else {
            TrainingStreakFeatured.Pattern(
                length = progress.length,
                mask = schedule.mask,
                mood = progress.mood,
                progressPercent = progress.percent,
                confidence = schedule.confidence,
            )
        }

        return Candidate(featured = featured, timeline = progress.timeline)
    }

    private fun detectSchedule(observation: Observation, period: Int): Schedule? {
        // The first recorded training fixes the phase; shifting a fitted mask must not
        // move today's cycle boundary to manufacture completed cycles or progress.
        val completedDays = observation.from.daysUntil(observation.completedThrough) + 1
        val completeCycles = completedDays / period
        if (completeCycles < MIN_COMPLETED_CYCLES) {
            return null
        }

        val evidenceCycles = minOf(completeCycles, PATTERN_HISTORY_CYCLES)
        val evidenceStart = observation.from.plus(
            DatePeriod(days = (completeCycles - evidenceCycles) * period)
        )
        val trainingCounts = IntArray(period)
        for (offset in 0 until evidenceCycles * period) {
            val date = evidenceStart.plus(DatePeriod(days = offset))
            if (date in observation.activeDates) {
                trainingCounts[offset % period]++
            }
        }
        if (trainingCounts.sum() < MIN_PATTERN_TRAINING_DAYS) {
            return null
        }

        val mask = mutableListOf<Boolean>()
        for (count in trainingCounts) {
            val proportion = count.toFloat() / evidenceCycles
            when {
                proportion >= MIN_PATTERN_AGREEMENT -> mask += true
                proportion <= MAX_REST_TRAINING_PROPORTION -> mask += false
                else -> return null
            }
        }
        val target = mask.count { it }
        if (target == 0 || target == period) {
            return null
        }

        val expectedTrainingObservations = target * evidenceCycles
        val expectedRestObservations = (period - target) * evidenceCycles
        val trainingMatches = trainingCounts.indices.filter { mask[it] }.sumOf { trainingCounts[it] }
        val restMatches = trainingCounts.indices.filter { !mask[it] }
            .sumOf { evidenceCycles - trainingCounts[it] }
        // Separate agreement prevents the many rest days in a sparse series from
        // concealing missed training days behind a high overall accuracy.
        val confidence = minOf(
            trainingMatches.toFloat() / expectedTrainingObservations,
            restMatches.toFloat() / expectedRestObservations,
        )

        return Schedule(
            anchor = observation.from,
            periodDays = period,
            target = target,
            confidence = confidence,
            mask = mask,
        )
    }

    private data class PeriodProgress(
        val length: Int,
        val percent: Int,
        val mood: TrainingStreakMood,
        val timeline: List<TrainingStreakProgressEntry>,
    )

    private fun periodProgress(observation: Observation, schedule: Schedule): PeriodProgress {
        val counts = observation.activeDates.groupingBy { date ->
            schedule.anchor.daysUntil(date) / schedule.periodDays
        }.eachCount()
        val currentIndex = schedule.anchor.daysUntil(observation.date) / schedule.periodDays
        val currentStart = schedule.anchor.plus(DatePeriod(days = currentIndex * schedule.periodDays))
        val currentEnd = currentStart.plus(DatePeriod(days = schedule.periodDays - 1))
        val achieved = counts[currentIndex] ?: 0
        val currentIsComplete = currentEnd <= observation.completedThrough

        // An open period preserves the preceding streak until its deadline, and is
        // counted immediately once the required number of distinct days is recorded.
        var pointer = currentIndex
        if (achieved < schedule.target && !currentIsComplete) {
            pointer--
        }
        var length = 0
        while (pointer >= 0 && (counts[pointer] ?: 0) >= schedule.target) {
            length++
            pointer--
        }

        val todayAvailable = if (
            observation.isCurrentDayOpen && observation.date !in observation.activeDates
        ) {
            1
        } else {
            0
        }
        val availableDays = observation.date.daysUntil(currentEnd) + todayAvailable
        val canComplete = achieved + availableDays >= schedule.target
        val hasRecentActivity = length > 0 || achieved > 0 ||
                observation.lastTrainingGapDays <= schedule.periodDays
        val isOnTrack = achieved >= schedule.target ||
                (!currentIsComplete && canComplete && hasRecentActivity)
        val mood = when {
            isOnTrack && length >= 4 -> TrainingStreakMood.CrushingIt
            isOnTrack -> TrainingStreakMood.OnTrack
            observation.lastTrainingGapDays >= maxOf(14, schedule.periodDays * 2) -> TrainingStreakMood.Restart
            else -> TrainingStreakMood.Paused
        }
        val firstIndex = (currentIndex - 3).coerceAtLeast(0)
        val timeline = (firstIndex..currentIndex).map { index ->
            val from = schedule.anchor.plus(DatePeriod(days = index * schedule.periodDays))
            val to = from.plus(DatePeriod(days = schedule.periodDays - 1))
            progressEntry(
                observation = observation,
                from = from,
                to = to,
                achieved = counts[index] ?: 0,
                target = schedule.target,
            )
        }

        return PeriodProgress(
            length = length,
            percent = progressPercent(achieved = achieved, target = schedule.target),
            mood = mood,
            timeline = timeline,
        )
    }

    private fun progressEntry(
        observation: Observation,
        from: LocalDate,
        to: LocalDate,
        achieved: Int,
        target: Int,
    ): TrainingStreakProgressEntry {
        return TrainingStreakProgressEntry(
            achievedTrainingDays = achieved,
            targetTrainingDays = target,
            isPartial = from < observation.from || to > observation.completedThrough,
            from = DateTimeUtils.startOfDay(from),
            to = DateTimeUtils.endOfDay(to),
        )
    }

    private fun progressPercent(achieved: Int, target: Int): Int {
        return ((achieved.toFloat() / target).coerceIn(0f, 1f) * 100).roundToInt()
    }

    private fun weekStart(date: LocalDate): LocalDate {
        return date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))
    }
}
