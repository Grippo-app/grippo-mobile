package com.grippo.shared.root

import androidx.compose.runtime.Composable
import com.grippo.core.BaseComposeScreen
import com.grippo.design.core.AppTokens
import com.grippo.platform.core.backAnimation
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun RootScreen(
    component: RootComponent,
    state: RootState,
    loaders: ImmutableSet<RootLoader>,
    contract: RootContract,
) = BaseComposeScreen(AppTokens.colors.background.primary) {
    ChildStackCompose(
        stack = component.childStack,
        animation = backAnimation(
            backHandler = component.backHandler,
            onBack = contract::back
        ),
        content = { child -> child.instance.component.Render() }
    )
}