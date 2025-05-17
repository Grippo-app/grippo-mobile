package com.grippo.authorization.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun SplashScreen(
    state: SplashState,
    loaders: ImmutableSet<SplashLoader>,
    contract: SplashContract
) = BaseComposeScreen {
    Box(Modifier.fillMaxSize())
}