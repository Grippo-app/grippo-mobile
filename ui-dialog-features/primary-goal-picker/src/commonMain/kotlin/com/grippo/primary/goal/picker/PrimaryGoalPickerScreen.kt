package com.grippo.primary.goal.picker

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun PrimaryGoalPickerScreen(
    state: PrimaryGoalPickerState,
    loaders: ImmutableSet<PrimaryGoalPickerLoader>,
    contract: PrimaryGoalPickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        PrimaryGoalPickerScreen(
            state = PrimaryGoalPickerState(
                title = "What's your main goal?",
                value = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
            ),
            loaders = persistentSetOf(),
            contract = PrimaryGoalPickerContract.Empty,
        )
    }
}
