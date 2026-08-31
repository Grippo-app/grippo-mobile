package com.grippo.authorization.login

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun LoginScreen(
    state: LoginState,
    loaders: ImmutableSet<LoginLoader>,
    contract: LoginContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        LoginScreen(
            state = LoginState(
                email = EmailFormatState.Empty(),
                password = PasswordFormatState.Empty(),
                isGoogleLoginAvailable = true,
                isAppleLoginAvailable = true,
            ),
            loaders = persistentSetOf(LoginLoader.LoginByEmailButton),
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
                password = PasswordFormatState.of("qwerty123"),
                isGoogleLoginAvailable = false,
                isAppleLoginAvailable = false,
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
                password = PasswordFormatState.of("qwerty123"),
                isGoogleLoginAvailable = true,
                isAppleLoginAvailable = true,
            ),
            loaders = persistentSetOf(LoginLoader.LoginByEmailButton),
            contract = LoginContract.Empty
        )
    }
}
