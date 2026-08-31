package com.grippo.confirm.training.completion

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun ConfirmTrainingCompletionScreen(
    state: ConfirmTrainingCompletionState,
    loaders: ImmutableSet<ConfirmTrainingCompletionLoader>,
    contract: ConfirmTrainingCompletionContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ConfirmTrainingCompletionScreen(
            state = ConfirmTrainingCompletionState(
                duration = DurationFormatState.of(45.minutes)
            ),
            contract = ConfirmTrainingCompletionContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
