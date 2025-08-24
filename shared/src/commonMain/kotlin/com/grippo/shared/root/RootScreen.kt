package com.grippo.shared.root

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.core.platform.platformAnimation
import com.grippo.design.components.connection.snackbar.ConnectionSnackbar
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun RootScreen(
    component: RootComponent,
    state: RootState,
    loaders: ImmutableSet<RootLoader>,
    contract: RootContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    ConnectionSnackbar(state = state.connection)

    ChildStackCompose(
        modifier = Modifier.fillMaxSize(),
        stack = component.childStack,
        animation = platformAnimation(),
        content = { child -> child.instance.component.Render() }
    )
}