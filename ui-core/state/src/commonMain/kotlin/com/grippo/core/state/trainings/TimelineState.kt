package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.DigestState
import com.grippo.core.state.metrics.stubDigest
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.time.Duration

@Immutable
public enum class TrainingPosition {
    FIRST,
    MIDDLE,
    LAST,
    SINGLE,
    EMPTY,
}

@Immutable
public sealed interface TimelineState {
    public val key: String
    public val position: TrainingPosition

    /**
     * Items that belong to the daily timeline representation.
     */
    @Immutable
    public sealed interface Daily : TimelineState {
        /**
         * A header-level daily item (e.g. digest, date block).
         */
        @Immutable
        public sealed interface Header : Daily

        /**
         * Building blocks that stay on the timeline.
         */
        @Immutable
        public sealed interface Item : Daily

        /**
         * Entries that carry exercises.
         */
        @Immutable
        public sealed interface Exercise :
            Item {
            public val exerciseState: ExerciseState
            public val indexInTraining: Int
        }
    }

    /**
     * Weekly nodes, capable of representing a summary and a full training entry.
     */
    @Immutable
    public sealed interface Weekly : TimelineState

    /**
     * Monthly nodes, capable of representing monthly summary and per-day training buckets.
     */
    @Immutable
    public sealed interface Monthly : TimelineState {
        public val month: LocalDate
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
        val createAt: LocalDateTime,
        val duration: Duration,
        val trainingId: String,
        override val position: TrainingPosition,
        override val key: String,
    ) : Daily.Item

    @Immutable
    public data class BetweenExercises(
        override val position: TrainingPosition,
        override val key: String,
    ) : Daily.Item

    /**
     * Weekly representation that exposes an entire training.
     */
    @Immutable
    public data class WeeklyTrainingsDay(
        val date: LocalDate,
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

    /**
     * Monthly representation that keeps all trainings for a day.
     */
    @Immutable
    public data class MonthlyDigest(
        val summary: DigestState,
        override val month: LocalDate,
        override val key: String,
        override val position: TrainingPosition = TrainingPosition.EMPTY,
    ) : Monthly

    @Immutable
    public data class MonthlyTrainingsDay(
        val date: LocalDate,
        override val month: LocalDate,
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
    val trainings = listOf(stubTraining(), stubTraining())
    if (trainings.isEmpty()) return persistentListOf()

    val values = mutableListOf<TimelineState>()
    trainings.forEachIndexed { index, training ->
        val position = when {
            trainings.size == 1 -> TrainingPosition.SINGLE
            index == 0 -> TrainingPosition.FIRST
            index == trainings.lastIndex -> TrainingPosition.LAST
            else -> TrainingPosition.MIDDLE
        }

        values += TimelineState.DateTime(
            createAt = training.createdAt,
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
    val monthRange = DateRange.Range.Monthly().range
    val monthReference = LocalDate(monthRange.from.year, monthRange.from.month, 1)
    val digest = stubDigest()
    val values = mutableListOf<TimelineState>()

    values += TimelineState.MonthlyDigest(
        summary = digest,
        month = monthReference,
        key = "stub-monthly-digest",
    )

    val dayOffsets = listOf(0, 3, 10, 18)
    val days = dayOffsets.mapIndexed { index, offset ->
        val date = monthReference.plus(DatePeriod(days = offset))
        val trainings = List(index % 2 + 1) { stubTraining() }.toPersistentList()
        val position = when {
            dayOffsets.size == 1 -> TrainingPosition.SINGLE
            index == 0 -> TrainingPosition.FIRST
            index == dayOffsets.lastIndex -> TrainingPosition.LAST
            else -> TrainingPosition.MIDDLE
        }

        TimelineState.MonthlyTrainingsDay(
            date = date,
            month = monthReference,
            trainings = trainings,
            key = "stub-monthly-day-$date",
            position = position,
        )
    }

    values += days
    return values.toPersistentList()
}

private fun TrainingState.toPreviewTrainingListValues(
    position: TrainingPosition,
): List<TimelineState> {
    if (exercises.isEmpty()) return emptyList()
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

    return result
}
