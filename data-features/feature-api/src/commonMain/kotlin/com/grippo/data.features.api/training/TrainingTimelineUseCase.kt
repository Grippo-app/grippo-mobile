package com.grippo.data.features.api.training

import com.grippo.data.features.api.metrics.models.MonthlyDigest
import com.grippo.data.features.api.metrics.models.WeeklyDigest
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
        weeklyDigest: WeeklyDigest? = null,
        monthlyDigest: MonthlyDigest? = null,
    ): TrainingTimeline {
        val nonEmpty = trainings.filter { it.exercises.isNotEmpty() }
        if (nonEmpty.isEmpty()) return TrainingTimeline(emptyList())

        val days = range.from.date.daysUntil(range.to.date) + 1
        val values = when {
            days <= 1 -> nonEmpty.toDailyTimeline()
            days <= 7 -> nonEmpty.toWeeklyTimeline(
                summary = requireNotNull(weeklyDigest) {
                    "Weekly digest is required for weekly timeline range."
                }
            )

            else -> nonEmpty.toMonthlyTimeline(
                digest = requireNotNull(monthlyDigest) {
                    "Monthly digest is required for monthly timeline range."
                }
            )
        }

        return TrainingTimeline(values)
    }

    public fun trainingExercises(
        training: Training,
        position: TrainingTimelinePosition = TrainingTimelinePosition.SINGLE,
    ): TrainingTimeline {
        return TrainingTimeline(training.toTrainingTimelineValues(position))
    }

    private fun List<Training>.toDailyTimeline(): List<TrainingTimelineValue> {
        val groupedByDate = groupBy { it.createdAt.date }
        val sortedDates = groupedByDate.keys.sortedDescending()

        return buildList {
            sortedDates.forEach { date ->
                val trainingsForDate = groupedByDate.getValue(date)
                trainingsForDate.forEachIndexed { index, training ->
                    val position = when {
                        trainingsForDate.size == 1 -> TrainingTimelinePosition.SINGLE
                        index == 0 -> TrainingTimelinePosition.FIRST
                        index == trainingsForDate.lastIndex -> TrainingTimelinePosition.LAST
                        else -> TrainingTimelinePosition.MIDDLE
                    }

                    add(
                        TrainingTimelineValue.DateTime(
                            createdAt = training.createdAt,
                            duration = training.duration,
                            trainingId = training.id,
                            position = position,
                            key = "date-${training.id}",
                        )
                    )

                    addAll(training.toTrainingTimelineValues(position))
                }
            }
        }
    }

    private fun List<Training>.toWeeklyTimeline(
        summary: WeeklyDigest,
    ): List<TrainingTimelineValue> {
        val groupedByDate = sortedByDescending { it.createdAt }.groupBy { it.createdAt.date }
        val sortedDates = groupedByDate.keys.sortedDescending()

        val values = mutableListOf<TrainingTimelineValue>()
        values += TrainingTimelineValue.WeeklySummary(
            summary = summary,
            key = "weekly-summary-${summary.weekStart}",
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
                trainings = trainingsForDate.sortedByDescending { it.createdAt },
                position = position,
                key = "weekly-day-$date",
            )
        }

        return values
    }

    private fun List<Training>.toMonthlyTimeline(
        digest: MonthlyDigest,
    ): List<TrainingTimelineValue> {
        val groupedByDate = groupBy { it.createdAt.date }
        val sortedDates = groupedByDate.keys.sortedDescending()
        val values = mutableListOf<TrainingTimelineValue>()

        values += TrainingTimelineValue.MonthSummary(
            summary = digest,
            month = digest.month,
            key = "monthly-digest-${digest.month.year}-${digest.month.month.number}",
        )

        sortedDates.forEachIndexed { index, date ->
            val trainingsForDate = groupedByDate.getValue(date).sortedBy { it.createdAt }
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

    private fun Training.toTrainingTimelineValues(
        position: TrainingTimelinePosition,
    ): List<TrainingTimelineValue> {
        if (exercises.isEmpty()) return emptyList()

        val result = mutableListOf<TrainingTimelineValue>()
        when (exercises.size) {
            1 -> {
                val exercise = exercises[0]
                result += TrainingTimelineValue.SingleExercise(
                    exercise = exercise,
                    position = position,
                    key = "single-$id-${exercise.id}",
                    indexInTraining = 1,
                )
            }

            2 -> {
                val first = exercises[0]
                val last = exercises[1]
                result += TrainingTimelineValue.FirstExercise(
                    exercise = first,
                    position = position,
                    key = "first-$id-${first.id}",
                    indexInTraining = 1,
                )
                result += TrainingTimelineValue.BetweenExercises(
                    position = position,
                    key = "between-$id-${first.id}-${last.id}",
                )
                result += TrainingTimelineValue.LastExercise(
                    exercise = last,
                    position = position,
                    key = "last-$id-${last.id}",
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
                    key = "first-$id-${first.id}",
                    indexInTraining = 1,
                )

                result += TrainingTimelineValue.BetweenExercises(
                    position = position,
                    key = "between-$id-${first.id}-${middle.first().id}",
                )

                middle.forEachIndexed { index, exercise ->
                    val indexInTraining = index + 2
                    result += TrainingTimelineValue.MiddleExercise(
                        exercise = exercise,
                        position = position,
                        key = "middle-$id-${exercise.id}",
                        indexInTraining = indexInTraining,
                    )

                    val next = middle.getOrNull(index + 1) ?: last
                    result += TrainingTimelineValue.BetweenExercises(
                        position = position,
                        key = "between-$id-${exercise.id}-${next.id}",
                    )
                }

                result += TrainingTimelineValue.LastExercise(
                    exercise = last,
                    position = position,
                    key = "last-$id-${last.id}",
                    indexInTraining = exercises.size,
                )
            }
        }

        return result
    }
}
