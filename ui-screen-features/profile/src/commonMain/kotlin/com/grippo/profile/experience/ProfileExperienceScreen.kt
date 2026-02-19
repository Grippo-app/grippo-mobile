package com.grippo.profile.experience

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCard
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.apply_btn
import com.grippo.design.resources.provider.choose_your_experience_level
import com.grippo.design.resources.provider.profile_menu_experience
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileExperienceScreen(
    state: ProfileExperienceState,
    loaders: ImmutableSet<ProfileExperienceLoader>,
    contract: ProfileExperienceContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen
    )
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.profile_menu_experience),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack),
    )

    Spacer(Modifier.height(AppTokens.dp.contentPadding.block))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.choose_your_experience_level),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

    val basePadding = PaddingValues(
        start = AppTokens.dp.screen.horizontalPadding,
        end = AppTokens.dp.screen.horizontalPadding,
        top = AppTokens.dp.contentPadding.content
    )

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = basePadding,
        overlay = AppTokens.colors.background.screen,
        content = { containerModifier, resolvedPadding ->
            LazyColumn(
                modifier = containerModifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                contentPadding = resolvedPadding
            ) {
                items(
                    items = state.suggestions,
                    key = { it.ordinal },
                ) { item ->
                    val selectProvider = remember(item) {
                        { contract.onSelectExperience(item) }
                    }
                    val isSelected = remember(state.selected, item) {
                        state.selected == item
                    }

                    CheckSelectableCard(
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = selectProvider,
                        isSelected = isSelected,
                        style = CheckSelectableCardStyle.Large(
                            title = item.title().text(),
                            description = item.description().text(),
                            icon = item.icon(),
                        ),
                    )
                }
            }
        },
        bottom = {
            val buttonState = remember(loaders, state.selected) {
                when {
                    loaders.contains(ProfileExperienceLoader.ApplyButton) -> ButtonState.Loading
                    state.selected == null -> ButtonState.Disabled
                    else -> ButtonState.Enabled
                }
            }

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

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

@AppPreview
@Composable
private fun ProfileExperienceScreenPreview() {
    PreviewContainer {
        ProfileExperienceScreen(
            state = ProfileExperienceState(
                selected = ExperienceEnumState.INTERMEDIATE
            ),
            loaders = persistentSetOf(),
            contract = ProfileExperienceContract.Empty
        )
    }
}
