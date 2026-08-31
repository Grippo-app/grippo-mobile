package com.grippo.draft.training

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun DraftTrainingScreen(
    state: DraftTrainingState,
    loaders: ImmutableSet<DraftTrainingLoader>,
    contract: DraftTrainingContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreviewAdd() {
    PreviewContainer {
        DraftTrainingScreen(
            state = DraftTrainingState(
                exercises = stubExercises(),
                stage = StageState.Add
            ),
            contract = DraftTrainingContract.Empty,
            loaders = persistentSetOf()
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewEdit() {
    PreviewContainer {
        DraftTrainingScreen(
            state = DraftTrainingState(
                exercises = stubExercises(),
                stage = StageState.Edit("")
            ),
            contract = DraftTrainingContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
