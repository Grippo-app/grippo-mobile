package com.grippo.trainings.factory

import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.TrainingPosition
import com.grippo.design.components.datetime.TimelineStyle

internal fun TimelineState.timelineStyle(): TimelineStyle = when (this) {
    is TimelineState.DateTime -> when (position) {
        TrainingPosition.FIRST -> TimelineStyle.Start
        TrainingPosition.MIDDLE -> TimelineStyle.Middle
        TrainingPosition.LAST -> TimelineStyle.End
        TrainingPosition.SINGLE -> TimelineStyle.Single
        TrainingPosition.EMPTY -> TimelineStyle.Empty
    }

    else -> when (this.position) {
        TrainingPosition.FIRST, TrainingPosition.MIDDLE -> TimelineStyle.Line
        TrainingPosition.LAST, TrainingPosition.SINGLE -> TimelineStyle.Empty
        TrainingPosition.EMPTY -> TimelineStyle.Empty
    }
}
