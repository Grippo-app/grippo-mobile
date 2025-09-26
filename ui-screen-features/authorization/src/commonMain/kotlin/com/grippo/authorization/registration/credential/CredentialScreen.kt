package com.grippo.authorization.registration.credential

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
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputEmail
import com.grippo.design.components.inputs.InputPassword
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.next_btn
import com.grippo.design.resources.provider.registration_credential_description
import com.grippo.design.resources.provider.registration_credential_title
import com.grippo.state.formatters.EmailFormatState
import com.grippo.state.formatters.PasswordFormatState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun CredentialScreen(
    state: CredentialState,
    loaders: ImmutableSet<CredentialLoader>,
    contract: CredentialContract
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
            text = AppTokens.strings.res(Res.string.registration_credential_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_credential_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        InputEmail(
            value = state.email.display,
            onValueChange = contract::onEmailChange
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        InputPassword(
            value = state.password.display,
            onValueChange = contract::onPasswordChange
        )

        Spacer(modifier = Modifier.weight(1f))

        val buttonState = remember(loaders, state.email, state.password) {
            when {
                state.email is EmailFormatState.Invalid -> ButtonState.Disabled
                state.email is EmailFormatState.Empty -> ButtonState.Disabled
                state.password is PasswordFormatState.Empty -> ButtonState.Disabled
                state.password is PasswordFormatState.Invalid -> ButtonState.Disabled
                else -> ButtonState.Enabled
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.next_btn),
            ),
            state = buttonState,
            style = ButtonStyle.Primary,
            onClick = contract::onNextClick
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        CredentialScreen(
            state = CredentialState(
                email = EmailFormatState.of(""),
                password = PasswordFormatState.of("")
            ),
            loaders = persistentSetOf(),
            contract = CredentialContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewFilled() {
    PreviewContainer {
        CredentialScreen(
            state = CredentialState(
                email = EmailFormatState.of("user@email.com"),
                password = PasswordFormatState.of("qwerty123")
            ),
            loaders = persistentSetOf(),
            contract = CredentialContract.Empty
        )
    }
}