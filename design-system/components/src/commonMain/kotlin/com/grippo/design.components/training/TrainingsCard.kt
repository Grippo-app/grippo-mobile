package com.grippo.design.components.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.training.internal.TrainingsCardMonthly
import com.grippo.design.components.training.internal.TrainingsCardWeekly
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public sealed interface TrainingsCardStyle {
    @Immutable
    public data object Weekly : TrainingsCardStyle

    @Immutable
    public data object Monthly : TrainingsCardStyle
}

@Composable
public fun TrainingsCard(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingState>,
    style: TrainingsCardStyle,
) {
    if (trainings.isEmpty()) return

    when (style) {
        TrainingsCardStyle.Weekly -> TrainingsCardWeekly(
            modifier = modifier,
            trainings = trainings
        )

        TrainingsCardStyle.Monthly -> TrainingsCardMonthly(
            modifier = modifier,
            trainings = trainings
        )
    }
}

@AppPreview
@Composable
private fun TrainingsCardPreview() {
    PreviewContainer {
        TrainingsCard(
            trainings = persistentListOf(stubTraining(), stubTraining(), stubTraining()),
            style = TrainingsCardStyle.Monthly
        )

        TrainingsCard(
            trainings = persistentListOf(stubTraining(), stubTraining(), stubTraining()),
            style = TrainingsCardStyle.Weekly
        )
    }
}
