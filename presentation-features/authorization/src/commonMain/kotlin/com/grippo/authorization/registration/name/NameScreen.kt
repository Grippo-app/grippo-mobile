package com.grippo.authorization.registration.name

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputName
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.continue_btn
import com.grippo.design.resources.registration_name_description
import com.grippo.design.resources.registration_name_title
import com.grippo.state.formatters.NameFormatState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun NameScreen(
    state: NameState,
    loaders: ImmutableSet<NameLoader>,
    contract: NameContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        onBack = contract::onBack,
        style = ToolbarStyle.Transparent
    )

    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .weight(1f)
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.contentPadding.content
            ).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_name_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_name_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        InputName(
            value = state.name.value,
            onValueChange = contract::onNameChange
        )

        Spacer(modifier = Modifier.weight(1f))

        val buttonState = remember(loaders, state.name) {
            when {
                state.name is NameFormatState.Valid -> ButtonState.Enabled
                else -> ButtonState.Disabled
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.continue_btn),
            state = buttonState,
            style = ButtonStyle.Primary,
            onClick = contract::onNextClick
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        NameScreen(
            state = NameState(
                name = NameFormatState.of("Mark S.")
            ),
            loaders = persistentSetOf(),
            contract = NameContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        NameScreen(
            state = NameState(
                name = NameFormatState.of("")
            ),
            loaders = persistentSetOf(),
            contract = NameContract.Empty
        )
    }
}