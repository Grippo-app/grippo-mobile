package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_completed_description
import com.grippo.design.resources.provider.training_completed_title
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Immutable
@Serializable
internal data class TrainingCompletedState(
    val savedTrainingId: String? = null,
    @Transient
    val title: UiText = UiText.Res(value = Res.string.training_completed_title),
    @Transient
    val description: UiText = UiText.Res(value = Res.string.training_completed_description),
    @Transient
    val timeline: ImmutableList<TimelineState> = persistentListOf(),
    @Transient
    val training: TrainingState? = null,
    @Transient
    val muscleLoad: MuscleLoadSummaryState? = null,
)
