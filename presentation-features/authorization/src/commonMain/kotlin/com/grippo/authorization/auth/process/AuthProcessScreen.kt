package com.grippo.authorization.auth.process

import androidx.compose.runtime.Composable
import com.grippo.core.BaseComposeScreen
import com.grippo.design.core.AppTokens
import com.grippo.platform.core.platformAnimation
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun AuthProcessScreen(
    component: AuthProcessComponent,
    state: AuthProcessState,
    loaders: ImmutableSet<AuthProcessLoader>,
    contract: AuthProcessContract
) = BaseComposeScreen(AppTokens.colors.background.primary) {
    ChildStackCompose(
        stack = component.childStack,
        animation = platformAnimation(),
        content = { child -> child.instance.component.Render() }
    )
}