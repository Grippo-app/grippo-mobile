package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.digest.DailyDigestState
import com.grippo.core.state.digest.MonthlyDigestState
import com.grippo.core.state.digest.WeeklyDigestState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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
public sealed interface TrainingListValue {
    public val key: String
    public val position: TrainingPosition

    /**
     * Items that belong to the daily timeline representation.
     */
    @Immutable
    public sealed interface Daily : TrainingListValue {
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
        public sealed interface Exercise : Item {
            public val exerciseState: ExerciseState
            public val indexInTraining: Int
        }
    }

    /**
     * Weekly nodes, capable of representing a summary and a full training entry.
     */
    @Immutable
    public sealed interface Weekly : TrainingListValue

    /**
     * Monthly nodes, capable of representing monthly summary and per-day training buckets.
     */
    @Immutable
    public sealed interface Monthly : TrainingListValue {
        public val month: LocalDate
    }

    @Immutable
    public data class DailyDigest(
        val state: DailyDigestState,
        override val key: String,
        override val position: TrainingPosition = TrainingPosition.EMPTY,
    ) : Daily.Header

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
        val summary: WeeklyDigestState,
        override val key: String,
        override val position: TrainingPosition = TrainingPosition.EMPTY,
    ) : Weekly

    /**
     * Monthly representation that keeps all trainings for a day.
     */
    @Immutable
    public data class MonthlyDigest(
        val summary: MonthlyDigestState,
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

    public companion object {
        public fun TrainingListValue.index(): Int? = (this as? Daily.Exercise)?.indexInTraining

        public fun TrainingListValue.exercise(): ExerciseState? =
            (this as? Daily.Exercise)?.exerciseState
    }
}
