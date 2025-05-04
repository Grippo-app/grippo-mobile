package com.grippo.authorization.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.password_placeholder
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun SplashScreen(
    state: SplashState,
    loaders: ImmutableSet<SplashLoader>,
    contract: SplashContract
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.password_placeholder),
            style = AppTokens.typography.b14Semi()
        )
    }
}