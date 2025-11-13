package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.digest.DailyDigestState
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
public sealed class TrainingListValue(
    public open val key: String,
    public open val position: TrainingPosition,
) {

    @Immutable
    public data class DailyDigest(
        val state: DailyDigestState,
        override val key: String,
    ) : TrainingListValue(key, position = TrainingPosition.EMPTY)

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
        val createAt: LocalDateTime,
        val duration: Duration,
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