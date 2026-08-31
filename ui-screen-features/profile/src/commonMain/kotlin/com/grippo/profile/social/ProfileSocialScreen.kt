package com.grippo.profile.social

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileSocialScreen(
    state: ProfileSocialState,
    loaders: ImmutableSet<ProfileSocialLoader>,
    contract: ProfileSocialContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ProfileSocialPreview() {
    PreviewContainer {
        ProfileSocialScreen(
            state = ProfileSocialState,
            loaders = persistentSetOf(),
            contract = ProfileSocialContract.Empty,
        )
    }
}
