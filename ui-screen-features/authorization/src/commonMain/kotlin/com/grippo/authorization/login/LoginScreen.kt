package com.grippo.authorization.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.design.components.brand.BrandHeader
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputEmail
import com.grippo.design.components.inputs.InputPassword
import com.grippo.design.components.spliter.ContentSpliter
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.continue_with_apple
import com.grippo.design.resources.provider.continue_with_google
import com.grippo.design.resources.provider.icons.Apple
import com.grippo.design.resources.provider.icons.Google
import com.grippo.design.resources.provider.login_button_login
import com.grippo.design.resources.provider.login_button_registration
import com.grippo.design.resources.provider.login_button_registration_label
import com.grippo.design.resources.provider.login_title
import com.grippo.design.resources.provider.or_continue_with
import com.grippo.design.resources.provider.track_real_progress_no_fluff
import com.grippo.services.apple.auth.rememberAppleAuthUiContext
import com.grippo.services.google.auth.rememberGoogleAuthUiContext
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun LoginScreen(
    state: LoginState,
    loaders: ImmutableSet<LoginLoader>,
    contract: LoginContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    val hasLoaders = loaders.isNotEmpty()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .statusBarsPadding()
            .imePadding()
            .padding(top = AppTokens.dp.screen.verticalPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandHeader(
                modifier = Modifier.fillMaxWidth(),
                title = AppTokens.strings.res(Res.string.login_title),
                description = AppTokens.strings.res(Res.string.track_real_progress_no_fluff),
                showLogo = true,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            InputEmail(
                value = state.email,
                onFocused = contract::onEmailFocused,
                onClear = contract::onEmailCleared,
                onNext = contract::onEmailKeyboardNext,
                onValueChange = contract::onEmailChange,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

            InputPassword(
                value = state.password,
                onFocused = contract::onPasswordFocused,
                onVisibilityChange = contract::onPasswordVisibilityChange,
                onDone = contract::onPasswordKeyboardDone,
                onValueChange = contract::onPasswordChange,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Spacer(modifier = Modifier.weight(1f))

            val buttonLoginByEmailState = when {
                loaders.contains(LoginLoader.LoginByEmailButton) -> ButtonState.Loading
                hasLoaders -> ButtonState.Disabled
                state.email !is EmailFormatState.Valid -> ButtonState.Disabled
                state.password !is PasswordFormatState.Valid -> ButtonState.Disabled
                else -> ButtonState.Enabled
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.login_button_login)),
                state = buttonLoginByEmailState,
                style = ButtonStyle.Primary,
                onClick = contract::onLoginByEmailClick,
            )
            if (state.isGoogleLoginAvailable || state.isAppleLoginAvailable) {
                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

                ContentSpliter(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.or_continue_with),
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isGoogleLoginAvailable) {
                    val googleAuthUiContext = rememberGoogleAuthUiContext()

                    val buttonLoginByGoogleState = when {
                        googleAuthUiContext == null -> ButtonState.Disabled
                        loaders.contains(LoginLoader.LoginByGoogleButton) -> ButtonState.Loading
                        hasLoaders -> ButtonState.Disabled
                        else -> ButtonState.Enabled
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        content = ButtonContent.Text(
                            text = AppTokens.strings.res(Res.string.continue_with_google),
                            startIcon = ButtonIcon.Image(AppTokens.icons.Google),
                        ),
                        state = buttonLoginByGoogleState,
                        style = ButtonStyle.Secondary,
                        onClick = { googleAuthUiContext?.let(contract::onLoginByGoogleClick) },
                    )
                }

                if (state.isAppleLoginAvailable) {
                    val appleAuthUiContext = rememberAppleAuthUiContext()

                    val buttonLoginByAppleState = when {
                        appleAuthUiContext == null -> ButtonState.Disabled
                        loaders.contains(LoginLoader.LoginByAppleButton) -> ButtonState.Loading
                        hasLoaders -> ButtonState.Disabled
                        else -> ButtonState.Enabled
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        content = ButtonContent.Text(
                            text = AppTokens.strings.res(Res.string.continue_with_apple),
                            startIcon = ButtonIcon.Icon(AppTokens.icons.Apple),
                        ),
                        state = buttonLoginByAppleState,
                        style = ButtonStyle.Secondary,
                        onClick = { appleAuthUiContext?.let(contract::onLoginByAppleClick) },
                    )
                }
            }

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                itemVerticalAlignment = Alignment.CenterVertically,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                horizontalArrangement = Arrangement.spacedBy(
                    space = AppTokens.dp.contentPadding.text,
                    alignment = Alignment.CenterHorizontally,
                ),
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
                    state = if (!hasLoaders) {
                        ButtonState.Enabled
                    } else {
                        ButtonState.Disabled
                    },
                    onClick = contract::onRegisterClick,
                )
            }

            Spacer(Modifier.height(AppTokens.dp.screen.verticalPadding))

            Spacer(Modifier.navigationBarsPadding())
        }
    }
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
            loaders = persistentSetOf(),
            contract = LoginContract.Empty,
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
            contract = LoginContract.Empty,
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
            contract = LoginContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewInvalid() {
    PreviewContainer {
        LoginScreen(
            state = LoginState(
                email = EmailFormatState.of("user@"),
                password = PasswordFormatState.of("qwerty123"),
                isGoogleLoginAvailable = true,
                isAppleLoginAvailable = false,
            ),
            loaders = persistentSetOf(),
            contract = LoginContract.Empty,
        )
    }
}
