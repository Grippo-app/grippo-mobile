package com.grippo.state.trainings

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    public open val key: String,
    public open val position: TrainingPosition,
) {

    @Immutable
    public data class FirstExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val key: String,
        val indexInTraining: Int
    ) : TrainingListValue(key, position)

    @Immutable
    public data class MiddleExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val key: String,
        val indexInTraining: Int
    ) : TrainingListValue(key, position)

    @Immutable
    public data class LastExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val key: String,
        val indexInTraining: Int
    ) : TrainingListValue(key, position)

    @Immutable
    public data class SingleExercise(
        val exerciseState: ExerciseState,
        override val position: TrainingPosition,
        override val key: String,
        val indexInTraining: Int
    ) : TrainingListValue(key, position)

    @Immutable
    public data class DateTime(
        val date: LocalDateTime,
        val trainingId: String,
        override val position: TrainingPosition,
        override val key: String,
    ) : TrainingListValue(key, position)

    @Immutable
    public data class BetweenExercises(
        override val position: TrainingPosition,
        override val key: String
    ) : TrainingListValue(key, position)

    public companion object {
        public fun TrainingListValue.shape(radius: Dp): RoundedCornerShape = when (this) {
            is FirstExercise -> RoundedCornerShape(
                topStart = radius,
                topEnd = radius
            )

            is SingleExercise -> RoundedCornerShape(
                topStart = radius,
                topEnd = radius,
                bottomEnd = radius,
                bottomStart = radius
            )

            is LastExercise -> RoundedCornerShape(
                bottomStart = radius,
                bottomEnd = radius
            )

            else -> RoundedCornerShape(0.dp)
        }

        public fun TrainingListValue.index(): Int? = when (this) {
            is FirstExercise -> indexInTraining
            is LastExercise -> indexInTraining
            is MiddleExercise -> indexInTraining
            is SingleExercise -> indexInTraining
            else -> null
        }

        public fun TrainingListValue.exercise(): ExerciseState? = when (this) {
            is FirstExercise -> exerciseState
            is LastExercise -> exerciseState
            is MiddleExercise -> exerciseState
            is SingleExercise -> exerciseState
            else -> null
        }
    }
}