package com.grippo.home.trainings.factory

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.models.Side
import com.grippo.design.components.timeline.TimeLinePointStyle
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingListValue
import com.grippo.state.trainings.TrainingPosition
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal fun timelineStyle(value: TrainingListValue): TimeLinePointStyle = when (value) {
    is TrainingListValue.DateTime -> when (value.position) {
        TrainingPosition.FIRST -> TimeLinePointStyle.Start
        TrainingPosition.MIDDLE -> TimeLinePointStyle.Middle
        TrainingPosition.LAST -> TimeLinePointStyle.End
        TrainingPosition.SINGLE -> TimeLinePointStyle.Single
    }

    else -> when (value.position) {
        TrainingPosition.FIRST, TrainingPosition.MIDDLE -> TimeLinePointStyle.Line
        TrainingPosition.LAST, TrainingPosition.SINGLE -> TimeLinePointStyle.Empty
    }
}

internal fun shapeFor(value: TrainingListValue, radius: Dp): RoundedCornerShape = when (value) {
    is TrainingListValue.FirstExercise -> RoundedCornerShape(
        topStart = radius,
        topEnd = radius
    )

    is TrainingListValue.SingleExercise -> RoundedCornerShape(
        topStart = radius,
        topEnd = radius
    )

    is TrainingListValue.TrainingSummary -> RoundedCornerShape(
        bottomStart = radius,
        bottomEnd = radius
    )

    else -> RoundedCornerShape(0.dp)
}

internal fun sidesFor(value: TrainingListValue): ImmutableList<Side> = when (value) {
    is TrainingListValue.FirstExercise -> persistentListOf(Side.TOP, Side.LEFT, Side.RIGHT)
    is TrainingListValue.SingleExercise -> persistentListOf(Side.TOP, Side.LEFT, Side.RIGHT)
    is TrainingListValue.LastExercise -> persistentListOf(Side.LEFT, Side.RIGHT)
    is TrainingListValue.MiddleExercise -> persistentListOf(Side.LEFT, Side.RIGHT)
    is TrainingListValue.TrainingSummary -> persistentListOf(Side.BOTTOM, Side.LEFT, Side.RIGHT)
    else -> persistentListOf()
}

internal fun exerciseOf(value: TrainingListValue): ExerciseState? = when (value) {
    is TrainingListValue.FirstExercise -> value.exerciseState
    is TrainingListValue.LastExercise -> value.exerciseState
    is TrainingListValue.MiddleExercise -> value.exerciseState
    is TrainingListValue.SingleExercise -> value.exerciseState
    else -> null
}