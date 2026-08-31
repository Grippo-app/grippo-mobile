package com.grippo.profile.body

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.profile.stubUser
import com.grippo.core.state.profile.stubWeightHistoryList
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ProfileBodyScreen(
    state: ProfileBodyState,
    loaders: ImmutableSet<ProfileBodyLoader>,
    contract: ProfileBodyContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ProfileBodyScreen(
            state = ProfileBodyState(
                weight = WeightFormatState.of(33f),
                height = HeightFormatState.of(90),
                history = stubWeightHistoryList(),
                user = stubUser()
            ),
            loaders = persistentSetOf(),
            contract = ProfileBodyContract.Empty
        )
    }
}
