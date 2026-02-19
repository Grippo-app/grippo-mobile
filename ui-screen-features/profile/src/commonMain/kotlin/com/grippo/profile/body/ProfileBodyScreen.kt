package com.grippo.profile.body

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
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.profile.stubUser
import com.grippo.core.state.profile.stubWeightHistoryList
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.inputs.InputHeight
import com.grippo.design.components.inputs.InputWeight
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.user.WeightHistoryCard
import com.grippo.design.components.user.WeightHistoryChart
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.apply_btn
import com.grippo.design.resources.provider.height
import com.grippo.design.resources.provider.weight
import com.grippo.design.resources.provider.weight_and_height
import com.grippo.design.resources.provider.weight_history
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileBodyScreen(
    state: ProfileBodyState,
    loaders: ImmutableSet<ProfileBodyLoader>,
    contract: ProfileBodyContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.weight_and_height),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack)
    )

    Spacer(Modifier.height(AppTokens.dp.contentPadding.block))

    Text(
        modifier = Modifier.padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        text = AppTokens.strings.res(Res.string.height),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.primary
    )

    Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

    InputHeight(
        modifier = Modifier.padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        value = state.height.display,
        onClick = contract::onHeightPickerClick
    )

    Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))

    Text(
        modifier = Modifier.padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        text = AppTokens.strings.res(Res.string.weight),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.primary
    )

    Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

    InputWeight(
        modifier = Modifier.padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        value = state.weight.display,
        onClick = contract::onWeightPickerClick
    )

    Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))

    Text(
        modifier = Modifier.padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        text = AppTokens.strings.res(Res.string.weight_history),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.primary
    )

    Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

    val basePadding = PaddingValues(
        top = AppTokens.dp.contentPadding.content,
        start = AppTokens.dp.screen.horizontalPadding,
        end = AppTokens.dp.screen.horizontalPadding,
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
                item {
                    WeightHistoryChart(
                        modifier = Modifier.fillMaxWidth(),
                        list = state.history
                    )
                }

                items(state.history) { item ->
                    WeightHistoryCard(
                        modifier = Modifier.fillMaxWidth(),
                        value = item
                    )
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            val buttonState = remember(loaders, state.user, state.weight, state.height) {
                val user = state.user ?: return@remember ButtonState.Disabled

                val hasNewWeight = user.weight.value != state.weight.value
                val hasNewHeight = user.height.value != state.height.value

                when {
                    loaders.contains(ProfileBodyLoader.ApplyBodyChanges) -> ButtonState.Loading
                    hasNewWeight || hasNewHeight -> ButtonState.Enabled
                    else -> ButtonState.Disabled
                }
            }

            Button(
                modifier = Modifier
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                    .fillMaxWidth(1f),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.apply_btn),
                ),
                style = ButtonStyle.Primary,
                state = buttonState,
                onClick = contract::onApplyClick
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    )
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ProfileBodyScreen(
            state = ProfileBodyState(
                weight = WeightFormatState.of(33f),
                height = HeightFormatState.of(90),
                history = stubWeightHistoryList(),
                user = stubUser()
            ),
            loaders = persistentSetOf(),
            contract = ProfileBodyContract.Empty
        )
    }
}