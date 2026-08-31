package com.grippo.authorization.splash

import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun SplashScreen(
    state: SplashState,
    loaders: ImmutableSet<SplashLoader>,
    contract: SplashContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        SplashScreen(
            state = SplashState,
            loaders = persistentSetOf(),
            contract = SplashContract.Empty
        )
    }
}
