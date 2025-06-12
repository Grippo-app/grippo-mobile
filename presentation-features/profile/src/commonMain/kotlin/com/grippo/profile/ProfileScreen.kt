package com.grippo.profile

import androidx.compose.runtime.Composable
import com.grippo.core.BaseComposeScreen
import com.grippo.design.core.AppTokens
import com.grippo.platform.core.platformAnimation
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun ProfileScreen(
    component: ProfileComponent,
    state: ProfileState,
    loaders: ImmutableSet<ProfileLoader>,
    contract: ProfileContract
) = BaseComposeScreen(AppTokens.colors.background.primary) {
    ChildStackCompose(
        stack = component.childStack,
        animation = platformAnimation(),
        content = { child -> child.instance.component.Render() }
    )
}