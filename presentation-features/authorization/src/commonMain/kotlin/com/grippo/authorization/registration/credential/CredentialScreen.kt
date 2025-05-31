package com.grippo.authorization.registration.credential

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputEmail
import com.grippo.design.components.inputs.InputPassword
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.next_btn
import com.grippo.design.resources.registration_credential_description
import com.grippo.design.resources.registration_credential_title
import com.grippo.presentation.api.auth.models.EmailFormatState
import com.grippo.presentation.api.auth.models.PasswordFormatState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun CredentialScreen(
    state: CredentialState,
    loaders: ImmutableSet<CredentialLoader>,
    contract: CredentialContract
) = BaseComposeScreen {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.screen.verticalPadding
            ).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(60.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_credential_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.registration_credential_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(26.dp))

        InputEmail(
            value = state.email.value,
            onValueChange = contract::setEmail
        )

        Spacer(modifier = Modifier.size(16.dp))

        InputPassword(
            value = state.password.value,
            onValueChange = contract::setPassword
        )

        Spacer(modifier = Modifier.weight(1f))

        val buttonState = remember(loaders, state.email, state.password) {
            when {
                state.email is EmailFormatState.Valid && state.password is PasswordFormatState.Valid -> ButtonState.Enabled
                else -> ButtonState.Disabled
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.next_btn),
            state = buttonState,
            style = ButtonStyle.Primary,
            onClick = contract::next
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