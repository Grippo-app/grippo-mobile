package com.grippo.profile.muscles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayLazyColumn
import com.grippo.design.components.muscle.MusclesColumn
import com.grippo.design.components.muscle.MusclesImage
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.apply_btn
import com.grippo.design.resources.provider.muscles
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun ProfileMusclesScreen(
    state: ProfileMusclesState,
    loaders: ImmutableSet<ProfileMusclesLoader>,
    contract: ProfileMusclesContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
        spot = ScreenBackground.Spot(
            top = AppTokens.colors.brand.color3,
            bottom = AppTokens.colors.brand.color2
        )
    )
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.muscles),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack),
    )

    BottomOverlayLazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(
            horizontal = AppTokens.dp.screen.horizontalPadding,
            vertical = AppTokens.dp.contentPadding.content
        ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        content = {
            itemsIndexed(
                state.suggestions,
                key = { _, item -> item.id }
            ) { index, group ->
                val isEven = index % 2 == 0
                val preset = state.musclePresets[group.id] ?: return@itemsIndexed

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEven) {
                        MusclesColumn(
                            modifier = Modifier.weight(1f),
                            item = group,
                            selectedIds = state.selectedMuscleIds,
                            onSelect = contract::onSelect
                        )
                        MusclesImage(
                            modifier = Modifier.weight(1f),
                            item = group,
                            preset = preset
                        )
                    } else {
                        MusclesImage(
                            modifier = Modifier.weight(1f),
                            item = group,
                            preset = preset
                        )
                        MusclesColumn(
                            modifier = Modifier.weight(1f),
                            item = group,
                            selectedIds = state.selectedMuscleIds,
                            onSelect = contract::onSelect
                        )
                    }
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

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

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    )
}
