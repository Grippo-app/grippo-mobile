package com.grippo.authorization.profile.creation.user

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun UserScreen(
    state: UserState,
    loaders: ImmutableSet<UserLoader>,
    contract: UserContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        UserScreen(
            state = UserState(
                weight = WeightFormatState.of(64.0f),
                height = HeightFormatState.of(144)
            ),
            loaders = persistentSetOf(),
            contract = UserContract.Empty
        )
    }
}
