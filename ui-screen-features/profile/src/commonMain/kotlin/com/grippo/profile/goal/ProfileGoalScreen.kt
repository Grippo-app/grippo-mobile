package com.grippo.profile.goal

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet

@Composable
internal fun ProfileGoalScreen(
    state: ProfileGoalState,
    loaders: ImmutableSet<ProfileGoalLoader>,
    contract: ProfileGoalContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ProfileGoalScreenPreview() {
    PreviewContainer {
        ProfileGoalScreen(
            state = ProfileGoalState(
                selectedPrimary = GoalPrimaryGoalEnumState.entries.random(),
                selectedSecondary = GoalSecondaryGoalEnumState.entries.random(),
                selectedPersonalization = PersonalizationKeyEnumState.entries
                    .shuffled()
                    .take(6)
                    .toPersistentSet(),
            ),
            loaders = persistentSetOf(),
            contract = ProfileGoalContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ProfileGoalScreenEmptyPreview() {
    PreviewContainer {
        ProfileGoalScreen(
            state = ProfileGoalState(),
            loaders = persistentSetOf(),
            contract = ProfileGoalContract.Empty,
        )
    }
}
