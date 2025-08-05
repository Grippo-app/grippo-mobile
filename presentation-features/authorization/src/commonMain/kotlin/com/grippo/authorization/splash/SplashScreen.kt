package com.grippo.authorization.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.loading.Loader
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun SplashScreen(
    state: SplashState,
    loaders: ImmutableSet<SplashLoader>,
    contract: SplashContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    Box(Modifier.fillMaxSize()) {
        if (loaders.contains(SplashLoader.AppContent)) {
            Loader(modifier = Modifier.fillMaxSize())
        }
    }
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