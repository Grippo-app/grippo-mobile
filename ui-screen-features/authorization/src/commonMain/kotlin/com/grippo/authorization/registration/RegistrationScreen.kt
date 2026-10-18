package com.grippo.authorization.registration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.design.components.brand.BrandHeader
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputEmail
import com.grippo.design.components.inputs.InputPassword
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.create_account_btn
import com.grippo.design.resources.provider.registration_description
import com.grippo.design.resources.provider.registration_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun RegistrationScreen(
    state: RegistrationState,
    loaders: ImmutableSet<RegistrationLoader>,
    contract: RegistrationContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        leading = Leading.Back(contract::onBack),
        style = ToolbarStyle.Transparent,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            BrandHeader(
                modifier = Modifier.fillMaxWidth(),
                title = AppTokens.strings.res(Res.string.registration_title),
                description = AppTokens.strings.res(Res.string.registration_description),
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

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = state.password.hint(),
                style = AppTokens.typography.b12Semi(),
                color = AppTokens.colors.text.tertiary,
                textAlign = TextAlign.End,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))
        }

        val buttonState = when {
            loaders.contains(RegistrationLoader.RegisterButton) -> ButtonState.Loading
            state.email !is EmailFormatState.Valid -> ButtonState.Disabled
            state.password !is PasswordFormatState.Valid -> ButtonState.Disabled
            else -> ButtonState.Enabled
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.create_account_btn),
            ),
            state = buttonState,
            style = ButtonStyle.Primary,
            onClick = contract::onNextClick,
        )

        Spacer(Modifier.height(AppTokens.dp.screen.verticalPadding))

        Spacer(Modifier.navigationBarsPadding())
    }
}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        RegistrationScreen(
            state = RegistrationState(
                email = EmailFormatState.Empty(),
                password = PasswordFormatState.Empty(),
            ),
            loaders = persistentSetOf(),
            contract = RegistrationContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewFilled() {
    PreviewContainer {
        RegistrationScreen(
            state = RegistrationState(
                email = EmailFormatState.of("user@email.com"),
                password = PasswordFormatState.of("qwerty123"),
            ),
            loaders = persistentSetOf(),
            contract = RegistrationContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewLoading() {
    PreviewContainer {
        RegistrationScreen(
            state = RegistrationState(
                email = EmailFormatState.of("user@email.com"),
                password = PasswordFormatState.of("qwerty123"),
            ),
            loaders = persistentSetOf(RegistrationLoader.RegisterButton),
            contract = RegistrationContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewInvalid() {
    PreviewContainer {
        RegistrationScreen(
            state = RegistrationState(
                email = EmailFormatState.of("user@"),
                password = PasswordFormatState.of("123"),
            ),
            loaders = persistentSetOf(),
            contract = RegistrationContract.Empty,
        )
    }
}
