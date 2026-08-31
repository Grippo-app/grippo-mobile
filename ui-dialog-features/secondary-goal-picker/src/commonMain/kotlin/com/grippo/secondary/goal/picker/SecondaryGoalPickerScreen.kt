package com.grippo.secondary.goal.picker

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun SecondaryGoalPickerScreen(
    state: SecondaryGoalPickerState,
    loaders: ImmutableSet<SecondaryGoalPickerLoader>,
    contract: SecondaryGoalPickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        SecondaryGoalPickerScreen(
            state = SecondaryGoalPickerState(
                title = "What else matters?",
                value = GoalSecondaryGoalEnumState.CONSISTENCY,
            ),
            loaders = persistentSetOf(),
            contract = SecondaryGoalPickerContract.Empty,
        )
    }
}
