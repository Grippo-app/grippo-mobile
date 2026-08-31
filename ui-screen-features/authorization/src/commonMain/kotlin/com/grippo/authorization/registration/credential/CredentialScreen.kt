package com.grippo.authorization.registration.credential

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
internal fun CredentialScreen(
    state: CredentialState,
    loaders: ImmutableSet<CredentialLoader>,
    contract: CredentialContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        CredentialScreen(
            state = CredentialState(
                email = EmailFormatState.Empty(),
                password = PasswordFormatState.Empty()
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
