package com.grippo.data.features.api.training

import com.grippo.data.features.api.metrics.engagement.models.TrainingDigestResult
import com.grippo.data.features.api.training.models.Training
import com.grippo.data.features.api.training.models.TrainingTimeline
import com.grippo.data.features.api.training.models.TrainingTimelinePosition
import com.grippo.data.features.api.training.models.TrainingTimelineValue
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.number

public class TrainingTimelineUseCase {

    public fun trainingTimeline(
        trainings: List<Training>,
        range: DateRange,
        digest: TrainingDigestResult? = null,
    ): TrainingTimeline {
        require(range.isWellFormed()) { "Training range must end at or after its start." }

        val selected = trainings
            .filter { it.createdAt >= range.from && it.createdAt <= range.to }
            .sortedWith(compareByDescending<Training> { it.createdAt }.thenBy { it.id })
        if (selected.isEmpty()) {
            return TrainingTimeline(emptyList())
        }

        val days = range.from.date.daysUntil(range.to.date) + 1
        val values = if (days == 1) {
            dailyTimeline(selected)
        } else {
            val summary = requireNotNull(digest) {
                "Digest is required for a timeline spanning multiple days."
            }
            require(summary.range == range) { "Digest range must match the timeline range." }

            if (days <= 7) {
                weeklyTimeline(trainings = selected, summary = summary)
            } else {
                monthlyTimeline(
                    trainings = selected,
                    digest = summary,
                    month = LocalDate(range.from.date.year, range.from.date.month, 1),
                )
            }
        }

        return TrainingTimeline(values)
    }

    public fun trainingExercises(
        training: Training,
        position: TrainingTimelinePosition = TrainingTimelinePosition.SINGLE,
    ): TrainingTimeline {
        val values = trainingTimelineValues(training = training, position = position)
        return TrainingTimeline(values)
    }

    private fun dailyTimeline(trainings: List<Training>): List<TrainingTimelineValue> {
        val groupedByDate = trainings.groupBy { it.createdAt.date }
        val sortedDates = groupedByDate.keys.sortedDescending()

        return buildList {
            sortedDates.forEach { date ->
                val trainingsForDate = groupedByDate.getValue(date)
                trainingsForDate.forEachIndexed { index, training ->
                    val duration = training.duration.takeIf { it.isFinite() && !it.isNegative() }
                    val position = when {
                        trainingsForDate.size == 1 -> TrainingTimelinePosition.SINGLE
                        index == 0 -> TrainingTimelinePosition.FIRST
                        index == trainingsForDate.lastIndex -> TrainingTimelinePosition.LAST
                        else -> TrainingTimelinePosition.MIDDLE
                    }

                    add(
                        TrainingTimelineValue.DateTime(
                            createdAt = training.createdAt,
                            duration = duration,
                            trainingId = training.id,
                            position = position,
                            key = "date-${training.id}",
                        )
                    )

                    val exercises = trainingTimelineValues(training = training, position = position)
                    addAll(exercises)
                }
            }
        }
    }

    private fun weeklyTimeline(
        trainings: List<Training>,
        summary: TrainingDigestResult,
    ): List<TrainingTimelineValue> {
        val groupedByDate = trainings.groupBy { it.createdAt.date }
        val sortedDates = groupedByDate.keys.sortedDescending()

        val values = mutableListOf<TrainingTimelineValue>()
        values += TrainingTimelineValue.WeeklySummary(
            summary = summary,
            key = "weekly-summary-${summary.range.from}-${summary.range.to}",
        )

        sortedDates.forEachIndexed { index, date ->
            val trainingsForDate = groupedByDate.getValue(date)
            val position = when {
                sortedDates.size == 1 -> TrainingTimelinePosition.SINGLE
                index == 0 -> TrainingTimelinePosition.FIRST
                index == sortedDates.lastIndex -> TrainingTimelinePosition.LAST
                else -> TrainingTimelinePosition.MIDDLE
            }

            values += TrainingTimelineValue.WeeklyTrainingsDay(
                date = date,
                trainings = trainingsForDate,
                position = position,
                key = "weekly-day-$date",
            )
        }

        return values
    }

    private fun monthlyTimeline(
        trainings: List<Training>,
        digest: TrainingDigestResult,
        month: LocalDate,
    ): List<TrainingTimelineValue> {
        val groupedByDate = trainings.groupBy { it.createdAt.date }
        val sortedDates = groupedByDate.keys.sortedDescending()
        val values = mutableListOf<TrainingTimelineValue>()

        values += TrainingTimelineValue.MonthSummary(
            summary = digest,
            month = month,
            key = "monthly-digest-${month.year}-${month.month.number}",
        )

        sortedDates.forEachIndexed { index, date ->
            val trainingsForDate = groupedByDate.getValue(date)
            val position = when {
                sortedDates.size == 1 -> TrainingTimelinePosition.SINGLE
                index == 0 -> TrainingTimelinePosition.FIRST
                index == sortedDates.lastIndex -> TrainingTimelinePosition.LAST
                else -> TrainingTimelinePosition.MIDDLE
            }

            val monthReference = LocalDate(date.year, date.month, 1)
            values += TrainingTimelineValue.MonthlyTrainingsDay(
                date = date,
                month = monthReference,
                trainings = trainingsForDate,
                position = position,
                key = "monthly-day-$date",
            )
        }

        return values
    }

    private fun trainingTimelineValues(
        training: Training,
        position: TrainingTimelinePosition,
    ): List<TrainingTimelineValue> {
        val exercises = training.exercises
        if (exercises.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<TrainingTimelineValue>()
        when (exercises.size) {
            1 -> {
                val exercise = exercises[0]
                result += TrainingTimelineValue.SingleExercise(
                    exercise = exercise,
                    position = position,
                    key = "single-${training.id}-${exercise.id}",
                    indexInTraining = 1,
                )
            }

            2 -> {
                val first = exercises[0]
                val last = exercises[1]
                result += TrainingTimelineValue.FirstExercise(
                    exercise = first,
                    position = position,
                    key = "first-${training.id}-${first.id}",
                    indexInTraining = 1,
                )
                result += TrainingTimelineValue.BetweenExercises(
                    position = position,
                    key = "between-${training.id}-${first.id}-${last.id}",
                )
                result += TrainingTimelineValue.LastExercise(
                    exercise = last,
                    position = position,
                    key = "last-${training.id}-${last.id}",
                    indexInTraining = 2,
                )
            }

            else -> {
                val first = exercises.first()
                val last = exercises.last()
                val middle = exercises.subList(1, exercises.lastIndex)

                result += TrainingTimelineValue.FirstExercise(
                    exercise = first,
                    position = position,
                    key = "first-${training.id}-${first.id}",
                    indexInTraining = 1,
                )

                result += TrainingTimelineValue.BetweenExercises(
                    position = position,
                    key = "between-${training.id}-${first.id}-${middle.first().id}",
                )

                middle.forEachIndexed { index, exercise ->
                    val indexInTraining = index + 2
                    result += TrainingTimelineValue.MiddleExercise(
                        exercise = exercise,
                        position = position,
                        key = "middle-${training.id}-${exercise.id}",
                        indexInTraining = indexInTraining,
                    )

                    val next = middle.getOrNull(index + 1) ?: last
                    result += TrainingTimelineValue.BetweenExercises(
                        position = position,
                        key = "between-${training.id}-${exercise.id}-${next.id}",
                    )
                }

                result += TrainingTimelineValue.LastExercise(
                    exercise = last,
                    position = position,
                    key = "last-${training.id}-${last.id}",
                    indexInTraining = exercises.size,
                )
            }
        }

        return result
    }
}
