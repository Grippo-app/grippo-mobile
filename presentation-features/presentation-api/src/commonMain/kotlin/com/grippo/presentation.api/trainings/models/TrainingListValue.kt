package com.grippo.presentation.api.trainings.models

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime

@Immutable
public enum class TrainingPosition {
    FIRST,
    MIDDLE,
    LAST,
    SINGLE,
}

@Immutable
public sealed class TrainingListValue(
    public open val position: TrainingPosition,
) {

    @Immutable
    public data class FirstExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition
    ) : TrainingListValue(position)

    @Immutable
    public data class MiddleExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition
    ) : TrainingListValue(position)

    @Immutable
    public data class LastExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition
    ) : TrainingListValue(position)

    @Immutable
    public data class SingleExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition
    ) : TrainingListValue(position)

    @Immutable
    public data class DateTime(
        val date: LocalDateTime,
        override val position: TrainingPosition
    ) : TrainingListValue(position)
}