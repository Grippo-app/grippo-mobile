package com.grippo.profile.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.profile.PersonalizationKeyEnumState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_save_btn
import com.grippo.design.resources.provider.goal_title
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet

@Composable
internal fun ProfileGoalScreen(
    state: ProfileGoalState,
    loaders: ImmutableSet<ProfileGoalLoader>,
    contract: ProfileGoalContract,
) = BaseComposeScreen(
    ScreenBackground.Color(AppTokens.colors.background.screen)
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.goal_title),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack),
    )

    val basePadding = PaddingValues(
        start = AppTokens.dp.screen.horizontalPadding,
        end = AppTokens.dp.screen.horizontalPadding,
        top = AppTokens.dp.contentPadding.content,
    )

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = basePadding,
        overlay = AppTokens.colors.background.screen,
        content = { containerModifier, resolvedPadding ->
            LazyColumn(
                modifier = containerModifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                contentPadding = resolvedPadding,
            ) {

            }
        },
        bottom = {
            Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

            val buttonState = remember(loaders) {
                when {
                    loaders.contains(ProfileGoalLoader.SaveButton) -> ButtonState.Loading
                    state.selectedPrimary == null -> ButtonState.Disabled
                    else -> ButtonState.Enabled
                }
            }

            Button(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.goal_save_btn),
                ),
                style = ButtonStyle.Primary,
                state = buttonState,
                onClick = contract::onSaveFastPath,
            )

            Spacer(Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(Modifier.navigationBarsPadding())
        },
    )
}

@AppPreview
@Composable
private fun ProfileGoalScreenPreview() {
    PreviewContainer {
        ProfileGoalScreen(
            state = ProfileGoalState(
                selectedPrimary = GoalPrimaryGoalEnumState.entries.random(),
                selectedSecondary = GoalSecondaryGoalEnumState.entries.random(),
                selectedTarget = DateTimeUtils.now(),
                selectedPersonalization = PersonalizationKeyEnumState.entries
                    .shuffled()
                    .take(3)
                    .toPersistentSet()
            ),
            loaders = persistentSetOf(),
            contract = ProfileGoalContract.Empty,
        )
    }
}
