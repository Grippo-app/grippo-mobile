package com.grippo.authorization.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputEmail
import com.grippo.design.components.inputs.InputPassword
import com.grippo.design.components.spliter.ContentSpliter
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.login_button_login
import com.grippo.design.resources.provider.login_button_registration
import com.grippo.design.resources.provider.login_button_registration_label
import com.grippo.design.resources.provider.login_description
import com.grippo.design.resources.provider.login_title
import com.grippo.design.resources.provider.or
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun LoginScreen(
    state: LoginState,
    loaders: ImmutableSet<LoginLoader>,
    contract: LoginContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
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
            text = AppTokens.strings.res(Res.string.login_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.login_description),
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
                loaders.contains(LoginLoader.LoginButton) -> ButtonState.Loading
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
                text = AppTokens.strings.res(Res.string.login_button_login),
            ),
            state = buttonState,
            style = ButtonStyle.Primary,
            onClick = contract::onLoginClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        ContentSpliter(
            text = AppTokens.strings.res(Res.string.or)
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                AppTokens.dp.contentPadding.text,
                Alignment.CenterHorizontally
            )
        ) {
            Text(
                text = AppTokens.strings.res(Res.string.login_button_registration_label),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
            )

            Button(
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.login_button_registration),
                ),
                size = ButtonSize.Small,
                style = ButtonStyle.Transparent,
                onClick = contract::onRegisterClick
            )
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        LoginScreen(
            state = LoginState(
                email = EmailFormatState.of(""),
                password = PasswordFormatState.of("")
            ),
            loaders = persistentSetOf(LoginLoader.LoginButton),
            contract = LoginContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewFilled() {
    PreviewContainer {
        LoginScreen(
            state = LoginState(
                email = EmailFormatState.of("user@email.com"),
                password = PasswordFormatState.of("qwerty123")
            ),
            loaders = persistentSetOf(),
            contract = LoginContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewLoading() {
    PreviewContainer {
        LoginScreen(
            state = LoginState(
                email = EmailFormatState.of("user@email.com"),
                password = PasswordFormatState.of("qwerty123")
            ),
            loaders = persistentSetOf(LoginLoader.LoginButton),
            contract = LoginContract.Empty
        )
    }
}