package com.grippo.data.features.api.training.models

import com.grippo.data.features.api.metrics.models.Digest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

public enum class TrainingTimelinePosition {
    FIRST,
    MIDDLE,
    LAST,
    SINGLE,
    EMPTY,
}

public data class TrainingTimeline(
    val values: List<TrainingTimelineValue>,
)

public sealed interface TrainingTimelineValue {
    public val key: String
    public val position: TrainingTimelinePosition

    public sealed interface Daily : TrainingTimelineValue {
        public sealed interface Item : Daily

        public sealed interface ExerciseValue : Item {
            public val exercise: Exercise
            public val indexInTraining: Int
        }
    }

    public sealed interface Weekly : TrainingTimelineValue

    public sealed interface Monthly : TrainingTimelineValue {
        public val month: LocalDate
    }

    public data class FirstExercise(
        override val exercise: Exercise,
        override val position: TrainingTimelinePosition,
        override val key: String,
        override val indexInTraining: Int,
    ) : Daily.ExerciseValue

    public data class MiddleExercise(
        override val exercise: Exercise,
        override val position: TrainingTimelinePosition,
        override val key: String,
        override val indexInTraining: Int,
    ) : Daily.ExerciseValue

    public data class LastExercise(
        override val exercise: Exercise,
        override val position: TrainingTimelinePosition,
        override val key: String,
        override val indexInTraining: Int,
    ) : Daily.ExerciseValue

    public data class SingleExercise(
        override val exercise: Exercise,
        override val position: TrainingTimelinePosition,
        override val key: String,
        override val indexInTraining: Int,
    ) : Daily.ExerciseValue

    public data class DateTime(
        val createdAt: LocalDateTime,
        val duration: Duration,
        val trainingId: String,
        override val position: TrainingTimelinePosition,
        override val key: String,
    ) : Daily.Item

    public data class BetweenExercises(
        override val position: TrainingTimelinePosition,
        override val key: String,
    ) : Daily.Item

    public data class WeeklyTrainingsDay(
        val date: LocalDate,
        val trainings: List<Training>,
        override val position: TrainingTimelinePosition,
        override val key: String,
    ) : Weekly

    public data class WeeklySummary(
        val summary: Digest,
        override val key: String,
        override val position: TrainingTimelinePosition = TrainingTimelinePosition.EMPTY,
    ) : Weekly

    public data class MonthSummary(
        val summary: Digest,
        override val month: LocalDate,
        override val key: String,
        override val position: TrainingTimelinePosition = TrainingTimelinePosition.EMPTY,
    ) : Monthly

    public data class MonthlyTrainingsDay(
        val date: LocalDate,
        override val month: LocalDate,
        val trainings: List<Training>,
        override val key: String,
        override val position: TrainingTimelinePosition,
    ) : Monthly
}
