package com.grippo.authorization.profile.creation.completed

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.loading.Loader
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileCompletedScreen(
    state: ProfileCompletedState,
    loaders: ImmutableSet<ProfileCompletedLoader>,
    contract: ProfileCompletedContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
    )
) {
    Loader(modifier = Modifier.fillMaxSize())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ProfileCompletedScreen(
            state = ProfileCompletedState,
            loaders = persistentSetOf(ProfileCompletedLoader.ProfileCreation),
            contract = ProfileCompletedContract.Empty
        )
    }
}
