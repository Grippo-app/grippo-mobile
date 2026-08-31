package com.grippo.training.completed

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.trainings.stubDailyTrainingTimeline
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingCompletedScreen(
    state: TrainingCompletedState,
    loaders: ImmutableSet<TrainingCompletedLoader>,
    contract: TrainingCompletedContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = TrainingCompletedState(
                timeline = stubDailyTrainingTimeline(),
                training = stubTraining(),
                muscleLoad = stubMuscleLoadSummary(),
            ),
            loaders = persistentSetOf(),
            contract = TrainingCompletedContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewLoading() {
    PreviewContainer {
        TrainingCompletedScreen(
            state = TrainingCompletedState(),
            loaders = persistentSetOf(TrainingCompletedLoader.SaveTraining),
            contract = TrainingCompletedContract.Empty
        )
    }
}
