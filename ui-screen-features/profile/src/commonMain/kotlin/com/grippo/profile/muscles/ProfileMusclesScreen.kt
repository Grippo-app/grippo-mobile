package com.grippo.profile.muscles

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.muscles.stubMuscleGroup
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.muscle.MuscleSelection
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.apply_btn
import com.grippo.design.resources.provider.muscles
import com.grippo.design.resources.provider.registration_muscles_description
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ProfileMusclesScreen(
    state: ProfileMusclesState,
    loaders: ImmutableSet<ProfileMusclesLoader>,
    contract: ProfileMusclesContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen
    )
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.muscles),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack),
    )

    Spacer(Modifier.height(AppTokens.dp.contentPadding.block))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.registration_muscles_description),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(AppTokens.dp.contentPadding.block))

    MuscleSelection(
        modifier = Modifier.fillMaxWidth().weight(1f),
        groups = state.suggestions,
        selectedGroupId = state.selectedGroupId,
        selectedMuscleIds = state.selectedMuscleIds,
        onGroupSelect = contract::onGroupClick,
        onMuscleSelect = contract::onSelect,
        bottom = {
            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))

            val buttonState = remember(loaders) {
                when {
                    loaders.contains(ProfileMusclesLoader.ApplyButton) -> ButtonState.Loading
                    else -> ButtonState.Enabled
                }
            }

            Button(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.apply_btn),
                ),
                style = ButtonStyle.Primary,
                state = buttonState,
                onClick = contract::onApply
            )

            Spacer(modifier = Modifier.height(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    )
}

@AppPreview
@Composable
private fun ProfileMusclesScreenPreview() {
    PreviewContainer {
        val groups = stubMuscleGroup()
        ProfileMusclesScreen(
            state = ProfileMusclesState(
                suggestions = groups,
                selectedGroupId = groups.first().id,
                selectedMuscleIds = groups.flatMap { it.muscles }.map { it.value.id }.toPersistentList()
            ),
            loaders = persistentSetOf(),
            contract = ProfileMusclesContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ProfileMusclesScreenLoadingPreview() {
    PreviewContainer {
        val groups = stubMuscleGroup()
        ProfileMusclesScreen(
            state = ProfileMusclesState(
                suggestions = groups,
                selectedGroupId = groups.first().id
            ),
            loaders = persistentSetOf(ProfileMusclesLoader.ApplyButton),
            contract = ProfileMusclesContract.Empty
        )
    }
}
