package com.grippo.shared.root

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.foundation.platform.platformStackAnimator
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
    ChildStackCompose(
        modifier = Modifier.fillMaxSize(),
        stack = component.childStack,
        animation = stackAnimation(
            selector = { child, _, _, _ -> child.instance.animator() }
        ),
        content = { child -> child.instance.component.Render() }
    )
}

private fun RootComponent.Child.animator(): StackAnimator =
    when (this) {
        is RootComponent.Child.Authorization -> fade()
        is RootComponent.Child.Home -> fade()
        is RootComponent.Child.Debug -> platformStackAnimator()
        is RootComponent.Child.Profile -> platformStackAnimator()
        is RootComponent.Child.Training -> platformStackAnimator()
        is RootComponent.Child.Trainings -> platformStackAnimator()
    }
