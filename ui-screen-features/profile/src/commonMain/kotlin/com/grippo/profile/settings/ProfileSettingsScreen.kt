package com.grippo.profile.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.delete_account
import com.grippo.design.resources.provider.settings
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileSettingsScreen(
    state: ProfileSettingsState,
    loaders: ImmutableSet<ProfileSettingsLoader>,
    contract: ProfileSettingsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.settings),
        style = ToolbarStyle.Transparent,
        leading = Leading.Back(contract::onBack)
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    val deleteState = remember(loaders) {
        when {
            loaders.contains(ProfileSettingsLoader.DeleteAccountButton) -> ButtonState.Loading
            else -> ButtonState.Enabled
        }
    }

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTokens.dp.screen.horizontalPadding),
        style = ButtonStyle.Error,
        state = deleteState,
        content = ButtonContent.Text(
            text = AppTokens.strings.res(Res.string.delete_account)
        ),
        onClick = contract::onDeleteAccount
    )
}

@AppPreview
@Composable
private fun ProfileSettingsPreview() {
    PreviewContainer {
        ProfileSettingsScreen(
            state = ProfileSettingsState,
            loaders = persistentSetOf(),
            contract = ProfileSettingsContract.Empty
        )
    }
}
