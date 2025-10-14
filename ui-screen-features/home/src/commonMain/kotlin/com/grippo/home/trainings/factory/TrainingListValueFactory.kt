package com.grippo.home.trainings.factory

import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.design.components.timeline.TimeLinePointStyle

internal fun TrainingListValue.timelineStyle(): TimeLinePointStyle = when (this) {
    is TrainingListValue.DateTime -> when (position) {
        TrainingPosition.FIRST -> TimeLinePointStyle.Start
        TrainingPosition.MIDDLE -> TimeLinePointStyle.Middle
        TrainingPosition.LAST -> TimeLinePointStyle.End
        TrainingPosition.SINGLE -> TimeLinePointStyle.Single
    }

    else -> when (this.position) {
        TrainingPosition.FIRST, TrainingPosition.MIDDLE -> TimeLinePointStyle.Line
        TrainingPosition.LAST, TrainingPosition.SINGLE -> TimeLinePointStyle.Empty
    }
}