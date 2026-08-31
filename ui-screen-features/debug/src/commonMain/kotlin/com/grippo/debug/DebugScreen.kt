package com.grippo.debug

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun DebugScreen(
    state: DebugState,
    loaders: ImmutableSet<DebugLoader>,
    contract: DebugContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        DebugScreen(
            state = DebugState(
                pushToken = "FCM-TOKEN"
            ),
            loaders = persistentSetOf(),
            contract = DebugContract.Empty
        )
    }
}
