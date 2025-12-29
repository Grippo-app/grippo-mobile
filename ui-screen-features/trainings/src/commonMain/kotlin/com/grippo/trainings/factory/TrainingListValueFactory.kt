package com.grippo.trainings.factory

import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.design.components.datetime.TimeLinePointStyle

internal fun TrainingListValue.timelineStyle(): TimeLinePointStyle = when (this) {
    is TrainingListValue.DateTime -> when (position) {
        TrainingPosition.FIRST -> TimeLinePointStyle.Start
        TrainingPosition.MIDDLE -> TimeLinePointStyle.Middle
        TrainingPosition.LAST -> TimeLinePointStyle.End
        TrainingPosition.SINGLE -> TimeLinePointStyle.Single
        TrainingPosition.EMPTY -> TimeLinePointStyle.Empty
    }

    else -> when (this.position) {
        TrainingPosition.FIRST, TrainingPosition.MIDDLE -> TimeLinePointStyle.Line
        TrainingPosition.LAST, TrainingPosition.SINGLE -> TimeLinePointStyle.Empty
        TrainingPosition.EMPTY -> TimeLinePointStyle.Empty
    }
}