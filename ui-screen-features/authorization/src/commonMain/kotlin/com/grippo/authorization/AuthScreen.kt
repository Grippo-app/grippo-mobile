package com.grippo.authorization

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
internal fun AuthScreen(
    component: AuthComponent,
    state: AuthState,
    loaders: ImmutableSet<AuthLoader>,
    contract: AuthContract
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

private fun AuthComponent.Child.animator(): StackAnimator =
    when (this) {
        is AuthComponent.Child.AuthProcess -> fade()
        is AuthComponent.Child.ProfileCreation -> platformStackAnimator()
        is AuthComponent.Child.Splash -> fade()
    }
