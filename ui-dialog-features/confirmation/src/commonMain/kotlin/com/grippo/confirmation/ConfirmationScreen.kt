package com.grippo.confirmation

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ConfirmationScreen(
    state: ConfirmationState,
    loaders: ImmutableSet<ConfirmationLoader>,
    contract: ConfirmationContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ConfirmationScreen(
            state = ConfirmationState(
                title = "Confirm Action",
                description = "Are you sure you want to proceed with this action? This cannot be undone."
            ),
            contract = ConfirmationContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
