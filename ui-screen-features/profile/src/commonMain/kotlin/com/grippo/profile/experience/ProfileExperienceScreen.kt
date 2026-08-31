package com.grippo.profile.experience

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileExperienceScreen(
    state: ProfileExperienceState,
    loaders: ImmutableSet<ProfileExperienceLoader>,
    contract: ProfileExperienceContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ProfileExperienceScreenPreview() {
    PreviewContainer {
        ProfileExperienceScreen(
            state = ProfileExperienceState(
                selected = ExperienceEnumState.INTERMEDIATE
            ),
            loaders = persistentSetOf(),
            contract = ProfileExperienceContract.Empty
        )
    }
}
