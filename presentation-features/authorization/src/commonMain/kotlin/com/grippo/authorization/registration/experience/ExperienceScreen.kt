package com.grippo.authorization.registration.experience

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.selectable.SelectableCard
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.continue_btn
import com.grippo.design.resources.registration_experience_description
import com.grippo.design.resources.registration_experience_title
import com.grippo.presentation.api.profile.models.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ExperienceScreen(
    state: ExperienceState,
    loaders: ImmutableSet<ExperienceLoader>,
    contract: ExperienceContract
) = BaseComposeScreen(AppTokens.colors.background.primary) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        onBack = contract::back,
        style = ToolbarStyle.Transparent
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.contentPadding.content
            ).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_experience_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_experience_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {

            items(state.suggestions, key = { it.ordinal }) { item ->
                val selectProvider = remember { { contract.select(item) } }
                val isSelected = remember(state.selected) { state.selected == item }

                SelectableCard(
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = selectProvider,
                    isSelected = isSelected,
                    style = SelectableCardStyle.Large(
                        title = item.title(),
                        description = item.description(),
                        icon = item.icon()
                    ),
                )
            }
        }

        val buttonState = remember(loaders, state.selected) {
            when {
                state.selected != null -> ButtonState.Enabled
                else -> ButtonState.Disabled
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.continue_btn),
            state = buttonState,
            style = ButtonStyle.Primary,
            onClick = contract::next
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