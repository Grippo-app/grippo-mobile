package com.grippo.authorization.profile.creation.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.NameFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputHeight
import com.grippo.design.components.inputs.InputName
import com.grippo.design.components.inputs.InputWeight
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.continue_btn
import com.grippo.design.resources.provider.registration_body_description
import com.grippo.design.resources.provider.registration_name_description
import com.grippo.design.resources.provider.registration_name_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun UserScreen(
    state: UserState,
    loaders: ImmutableSet<UserLoader>,
    contract: UserContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen
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
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_name_title),
//            text = AppTokens.strings.res(Res.string.registration_body_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_name_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))


        InputName(
            value = state.name.display,
            onValueChange = contract::onNameChange
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_body_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        InputWeight(
            value = state.weight.display,
            onClick = contract::onWeightPickerClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        InputHeight(
            value = state.height.display,
            onClick = contract::onHeightPickerClick
        )

        Spacer(modifier = Modifier.weight(1f))

        val buttonState = remember(loaders, state.name) {
            when {
                state.name is NameFormatState.Invalid -> ButtonState.Disabled
                state.name is NameFormatState.Empty -> ButtonState.Disabled
                else -> ButtonState.Enabled
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.continue_btn),
            ),
            state = buttonState,
            style = ButtonStyle.Primary,
            onClick = contract::onNextClick
        )

        Spacer(Modifier.height(AppTokens.dp.screen.verticalPadding))

        Spacer(Modifier.navigationBarsPadding())
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        UserScreen(
            state = UserState(
                weight = WeightFormatState.of(64.0f),
                height = HeightFormatState.of(144)
            ),
            loaders = persistentSetOf(),
            contract = UserContract.Empty
        )
    }
}