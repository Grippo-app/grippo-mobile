package com.grippo.profile.settings

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileSettingsScreen(
    state: ProfileSettingsState,
    loaders: ImmutableSet<ProfileSettingsLoader>,
    contract: ProfileSettingsContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

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
