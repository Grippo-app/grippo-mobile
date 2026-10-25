package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.minutes

@Immutable
public sealed interface TimelineState {
    public val key: String
    public val position: TrainingPosition

    @Immutable
    public sealed interface Daily : TimelineState {
        @Immutable
        public sealed interface Header : Daily

        @Immutable
        public sealed interface Item : Daily

        @Immutable
        public sealed interface Exercise :
            Item {
            public val exerciseState: ExerciseState
            public val indexInTraining: Int
        }
    }

    @Immutable
    public sealed interface Weekly : TimelineState

    @Immutable
    public sealed interface Monthly : TimelineState {
        public val month: DateFormatState
    }

    @Immutable
    public data class FirstExercise(
        override val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val key: String,
        override val indexInTraining: Int,
    ) : Daily.Exercise

    @Immutable
    public data class MiddleExercise(
        override val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val key: String,
        override val indexInTraining: Int,
    ) : Daily.Exercise

    @Immutable
    public data class LastExercise(
        override val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val key: String,
        override val indexInTraining: Int,
    ) : Daily.Exercise

    @Immutable
    public data class SingleExercise(
        override val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val key: String,
        override val indexInTraining: Int,
    ) : Daily.Exercise

    @Immutable
    public data class DateTime(
        val startAt: DateTimeFormatState,
        val createAt: DateTimeFormatState,
        val duration: DurationFormatState,
        val trainingId: String,
        override val position: TrainingPosition,
        override val key: String,
    ) : Daily.Item

    @Immutable
    public data class BetweenExercises(
        override val position: TrainingPosition,
        override val key: String,
    ) : Daily.Item

    @Immutable
    public data class WeeklyTrainingsDay(
        val date: DateFormatState,
        val trainings: ImmutableList<TrainingState>,
        override val position: TrainingPosition,
        override val key: String,
    ) : Weekly

    @Immutable
    public data class WeeklySummary(
        val summary: DigestState,
        override val key: String,
        override val position: TrainingPosition = TrainingPosition.EMPTY,
    ) : Weekly

    @Immutable
    public data class MonthlyDigest(
        val summary: DigestState,
        override val month: DateFormatState,
        override val key: String,
        override val position: TrainingPosition = TrainingPosition.EMPTY,
    ) : Monthly

    @Immutable
    public data class MonthlyTrainingsDay(
        val date: DateFormatState,
        override val month: DateFormatState,
        val trainings: ImmutableList<TrainingState>,
        override val key: String,
        override val position: TrainingPosition,
    ) : Monthly

    public companion object Companion {
        public fun TimelineState.index(): Int? = (this as? Daily.Exercise)?.indexInTraining

        public fun TimelineState.exercise(): ExerciseState? =
            (this as? Daily.Exercise)?.exerciseState
    }
}

public fun stubDailyTrainingTimeline(): ImmutableList<TimelineState> {
    val trainings = listOf(
        timelinePreviewTraining(id = "morning", createdAt = LocalDateTime(2026, 10, 16, 9, 45)),
        timelinePreviewTraining(id = "evening", createdAt = LocalDateTime(2026, 10, 16, 18, 45)),
    )

    val values = mutableListOf<TimelineState>()
    trainings.forEachIndexed { index, training ->
        val position = when {
            trainings.size == 1 -> TrainingPosition.SINGLE
            index == 0 -> TrainingPosition.FIRST
            index == trainings.lastIndex -> TrainingPosition.LAST
            else -> TrainingPosition.MIDDLE
        }

        val end = training.createdAt.value

        val durationValue = training.duration.value
        val start = end?.let { endValue ->
            durationValue?.let { DateTimeUtils.minus(endValue, it) }
        }

        values += TimelineState.DateTime(
            startAt = DateTimeFormatState.of(
                value = start,
                range = DateRangePresets.infinity(),
                format = DateFormat.TimeOnly.Time24hHm
            ),
            createAt = DateTimeFormatState.of(
                value = end,
                range = DateRangePresets.infinity(),
                format = DateFormat.TimeOnly.Time24hHm
            ),
            duration = training.duration,
            trainingId = training.id,
            position = position,
            key = "stub-date-${training.id}",
        )

        values += training.toPreviewTrainingListValues(position)
    }

    return values.toPersistentList()
}

public fun stubMonthlyTrainingTimeline(): ImmutableList<TimelineState> {
    val monthRange = DateRange(
        from = LocalDateTime(2026, 10, 1, 0, 0),
        to = LocalDateTime(2026, 10, 31, 23, 59, 59),
    )
    val monthReference = LocalDate(monthRange.from.year, monthRange.from.month, 1)
    val monthReferenceFormatted = DateFormatState.of(
        value = monthReference,
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.MmmYyyy,
    )
    val digest = DigestState.Content(
        trainingsCount = 5,
        duration = DurationFormatState.ofTotal(225.minutes),
        total = VolumeFormatState.ofTotal(9_600f),
        totalSets = 20,
    )
    val values = mutableListOf<TimelineState>()

    values += TimelineState.MonthlyDigest(
        summary = digest,
        month = monthReferenceFormatted,
        key = "stub-monthly-digest",
    )

    val dayOffsets = listOf(0, 3, 10, 15)
    val days = dayOffsets.mapIndexed { index, offset ->
        val date = monthReference.plus(DatePeriod(days = offset))
        val trainingCount = if (index == dayOffsets.lastIndex) {
            2
        } else {
            1
        }
        val trainings = List(trainingCount) { trainingIndex ->
            timelinePreviewTraining(
                id = "training-$index-$trainingIndex",
                createdAt = LocalDateTime(date = date, time = monthRange.from.time),
            )
        }.toPersistentList()
        val position = when {
            dayOffsets.size == 1 -> TrainingPosition.SINGLE
            index == 0 -> TrainingPosition.FIRST
            index == dayOffsets.lastIndex -> TrainingPosition.LAST
            else -> TrainingPosition.MIDDLE
        }

        TimelineState.MonthlyTrainingsDay(
            date = DateFormatState.of(
                value = date,
                range = DateRangePresets.infinity(),
                format = DateFormat.DateOnly.DateDdMmm,
            ),
            month = monthReferenceFormatted,
            trainings = trainings,
            key = "stub-monthly-day-$date",
            position = position,
        )
    }

    values += days
    return values.toPersistentList()
}

private fun timelinePreviewTraining(id: String, createdAt: LocalDateTime): TrainingState {
    val training = stubTraining()
    val baseExercise = stubExercise()
    val completed = stubPendingIteration().copy(
        id = "$id-set-1",
        externalWeight = VolumeFormatState.of(60f),
        repetitions = RepetitionsFormatState.of(8),
    )
    val exercise = baseExercise.copy(
        id = "$id-exercise-1",
        exerciseExample = baseExercise.exerciseExample.copy(id = "$id-example-1"),
        iterations = persistentListOf(completed, completed.copy(id = "$id-set-2")),
    )

    return training.copy(
        id = id,
        createdAt = DateTimeFormatState.of(
            value = createdAt,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy,
        ),
        duration = DurationFormatState.of(45.minutes),
        exercises = persistentListOf(
            exercise,
            exercise.copy(
                id = "$id-exercise-2",
                exerciseExample = exercise.exerciseExample.copy(
                    id = "$id-example-2",
                    name = "Жим гантелей лёжа на наклонной скамье нейтральным хватом",
                ),
            ),
        ),
    )
}

private fun TrainingState.toPreviewTrainingListValues(
    position: TrainingPosition,
): ImmutableList<TimelineState> {
    if (exercises.isEmpty()) {
        return persistentListOf()
    }

    val result = mutableListOf<TimelineState>()
    when (exercises.size) {
        1 -> {
            val exercise = exercises.first()
            result += TimelineState.SingleExercise(
                exerciseState = exercise,
                position = position,
                key = "stub-single-$id-${exercise.id}",
                indexInTraining = 1,
            )
        }

        2 -> {
            val first = exercises.first()
            val last = exercises.last()
            result += TimelineState.FirstExercise(
                exerciseState = first,
                position = position,
                key = "stub-first-$id-${first.id}",
                indexInTraining = 1,
            )
            result += TimelineState.BetweenExercises(
                position = position,
                key = "stub-between-$id-${first.id}-${last.id}",
            )
            result += TimelineState.LastExercise(
                exerciseState = last,
                position = position,
                key = "stub-last-$id-${last.id}",
                indexInTraining = 2,
            )
        }

        else -> {
            val first = exercises.first()
            val last = exercises.last()
            val middle = exercises.subList(1, exercises.lastIndex)

            result += TimelineState.FirstExercise(
                exerciseState = first,
                position = position,
                key = "stub-first-$id-${first.id}",
                indexInTraining = 1,
            )

            result += TimelineState.BetweenExercises(
                position = position,
                key = "stub-between-$id-${first.id}-${middle.first().id}",
            )

            middle.forEachIndexed { index, exercise ->
                val indexInTraining = index + 2
                result += TimelineState.MiddleExercise(
                    exerciseState = exercise,
                    position = position,
                    key = "stub-middle-$id-${exercise.id}",
                    indexInTraining = indexInTraining,
                )

                val next = middle.getOrNull(index + 1) ?: last
                result += TimelineState.BetweenExercises(
                    position = position,
                    key = "stub-between-$id-${exercise.id}-${next.id}",
                )
            }

            result += TimelineState.LastExercise(
                exerciseState = last,
                position = position,
                key = "stub-last-$id-${last.id}",
                indexInTraining = exercises.size,
            )
        }
    }

    return result.toPersistentList()
}
