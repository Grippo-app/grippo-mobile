package com.grippo.shared.dialog.content

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun DialogContentScreen(
    component: DialogContentComponent,
    state: DialogContentState,
    loaders: ImmutableSet<DialogContentLoader>,
    contract: DialogContentContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.secondary)) {
    ChildStackCompose(
        stack = component.childStack,
        animation = stackAnimation(animator = fade()),
        content = { child -> child.instance.component.Render() }
    )
}