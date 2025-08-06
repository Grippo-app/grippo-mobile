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
import androidx.compose.material3.HorizontalDivider
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
import com.grippo.design.components.inputs.InputEmail
import com.grippo.design.components.inputs.InputPassword
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.login_button_login
import com.grippo.design.resources.login_button_registration
import com.grippo.design.resources.login_button_registration_label
import com.grippo.design.resources.login_description
import com.grippo.design.resources.login_title
import com.grippo.design.resources.or
import com.grippo.state.auth.EmailFormatState
import com.grippo.state.profile.PasswordFormatState
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

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.login_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        InputEmail(
            value = state.email.value,
            onValueChange = contract::onEmailChange
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        InputPassword(
            value = state.password.value,
            onValueChange = contract::onPasswordChange
        )

        Spacer(modifier = Modifier.weight(1f))

        val buttonState = remember(loaders, state.email, state.password) {
            when {
                loaders.contains(LoginLoader.LoginButton) -> ButtonState.Loading
                state.email is EmailFormatState.Valid && state.password is PasswordFormatState.Valid -> ButtonState.Enabled
                else -> ButtonState.Disabled
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.login_button_login),
            state = buttonState,
            style = ButtonStyle.Primary,
            onClick = contract::onLoginClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTokens.colors.divider.default
            )

            Text(
                text = AppTokens.strings.res(Res.string.or),
                style = AppTokens.typography.b14Reg(),
                color = AppTokens.colors.text.primary,
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTokens.colors.divider.default
            )
        }

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
                style = AppTokens.typography.b14Reg(),
                color = AppTokens.colors.text.secondary,
            )

            Button(
                text = AppTokens.strings.res(Res.string.login_button_registration),
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