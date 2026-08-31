package com.grippo.goal.setup.suggestion

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun GoalSetupSuggestionScreen(
    state: GoalSetupSuggestionState,
    loaders: ImmutableSet<GoalSetupSuggestionLoader>,
    contract: GoalSetupSuggestionContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun GoalSetupSuggestionScreenPreview() {
    PreviewContainer {
        GoalSetupSuggestionScreen(
            state = GoalSetupSuggestionState(),
            loaders = persistentSetOf(),
            contract = GoalSetupSuggestionContract.Empty,
        )
    }
}
