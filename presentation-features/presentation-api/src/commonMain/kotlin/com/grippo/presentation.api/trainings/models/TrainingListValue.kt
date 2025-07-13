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
    public open val id: String,
    public open val position: TrainingPosition,
) {

    @Immutable
    public data class FirstExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val id: String
    ) : TrainingListValue(id, position)

    @Immutable
    public data class MiddleExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val id: String
    ) : TrainingListValue(id, position)

    @Immutable
    public data class LastExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val id: String
    ) : TrainingListValue(id, position)

    @Immutable
    public data class SingleExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val id: String
    ) : TrainingListValue(id, position)

    @Immutable
    public data class DateTime(
        val date: LocalDateTime,
        override val position: TrainingPosition,
        override val id: String
    ) : TrainingListValue(id, position)

    @Immutable
    public data class BetweenExercises(
        override val position: TrainingPosition,
        override val id: String
    ) : TrainingListValue(id, position)
}