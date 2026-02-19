package com.grippo.profile.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.toolbar.Leading
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.components.user.DeleteUserCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
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

    Spacer(modifier = Modifier.weight(1f))

    DeleteUserCard(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        isDeleteLoading = loaders.contains(ProfileSettingsLoader.DeleteAccountButton),
        onDeleteClick = contract::onDeleteAccount
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

    Spacer(modifier = Modifier.navigationBarsPadding())
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
