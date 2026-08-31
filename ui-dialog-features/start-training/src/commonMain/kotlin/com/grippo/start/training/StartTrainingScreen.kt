package com.grippo.start.training

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.trainings.stubExercises
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun StartTrainingScreen(
    state: StartTrainingState,
    loaders: ImmutableSet<StartTrainingLoader>,
    contract: StartTrainingContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            StartTrainingScreen(
                state = StartTrainingState(),
                contract = StartTrainingContract.Empty,
                loaders = persistentSetOf(),
            )
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreviewWithOptions() {
    PreviewContainer {
        StartTrainingScreen(
            state = StartTrainingState(
                options = persistentListOf(
                    StartTrainingOption.Empty,
                    StartTrainingOption.Preset(stubExercises()),
                    StartTrainingOption.Recent(
                        trainingId = "stub-1",
                        createdAt = DateTimeFormatState.of(
                            value = DateTimeUtils.now(),
                            range = DateRangePresets.infinity(),
                            format = DateFormat.DateOnly.DateMmmDdYyyy,
                        ),
                        exercises = stubExercises(),
                    ),
                ),
            ),
            contract = StartTrainingContract.Empty,
            loaders = persistentSetOf(),
        )
    }
}
