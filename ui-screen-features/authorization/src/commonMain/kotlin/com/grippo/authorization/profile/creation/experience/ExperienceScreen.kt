package com.grippo.authorization.profile.creation.experience

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
internal fun ExperienceScreen(
    state: ExperienceState,
    loaders: ImmutableSet<ExperienceLoader>,
    contract: ExperienceContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ScreenPreviewSelected() {
    PreviewContainer {
        ExperienceScreen(
            state = ExperienceState(
                selected = ExperienceEnumState.INTERMEDIATE
            ),
            loaders = persistentSetOf(),
            contract = ExperienceContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewEmpty() {
    PreviewContainer {
        ExperienceScreen(
            state = ExperienceState(),
            loaders = persistentSetOf(),
            contract = ExperienceContract.Empty
        )
    }
}
