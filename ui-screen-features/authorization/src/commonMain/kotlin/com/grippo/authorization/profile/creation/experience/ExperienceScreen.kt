package com.grippo.authorization.profile.creation.experience

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.cards.selectable.SelectableCard
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.continue_btn
import com.grippo.design.resources.provider.registration_experience_description
import com.grippo.design.resources.provider.registration_experience_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExperienceScreen(
    state: ExperienceState,
    loaders: ImmutableSet<ExperienceLoader>,
    contract: ExperienceContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
    )
) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        leading = Leading.Back(contract::onBack),
        style = ToolbarStyle.Transparent
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_experience_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_experience_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        val basePadding = PaddingValues(horizontal = AppTokens.dp.screen.horizontalPadding)

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
                        val selectProvider = remember(item) { { contract.onExperienceClick(item) } }
                        val isSelected = remember(state.selected, item) { state.selected == item }

                        SelectableCard(
                            modifier = Modifier.fillMaxWidth(),
                            onSelect = selectProvider,
                            isSelected = isSelected,
                            style = CheckSelectableCardStyle.Large(
                                title = item.title().text(),
                                description = item.description().text(),
                                icon = item.icon(),
                                subContent = null,
                            ),
                        )
                    }
                }
            },
            bottom = {
                val buttonState = remember(loaders, state.selected) {
                    when {
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
                        text = AppTokens.strings.res(Res.string.continue_btn),
                    ),
                    state = buttonState,
                    style = ButtonStyle.Primary,
                    onClick = contract::onNextClick
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewSelected() {
    PreviewContainer {
        ExperienceScreen(
            state = ExperienceState(
                selected = ExperienceEnumState.INTERMEDIATE
            ),
            loaders = persistentSetOf(),
            contract = ExperienceContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        ExperienceScreen(
            state = ExperienceState(),
            loaders = persistentSetOf(),
            contract = ExperienceContract.Empty
        )
    }
}
