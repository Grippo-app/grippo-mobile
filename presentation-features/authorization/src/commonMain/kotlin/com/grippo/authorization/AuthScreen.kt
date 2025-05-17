package com.grippo.authorization

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.grippo.authorization.AuthComponent.Child
import com.grippo.core.BaseComposeScreen
import com.grippo.presentation.api.auth.AuthRouter
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun AuthScreen(
    stack: Value<ChildStack<AuthRouter, Child>>,
    state: AuthState,
    loaders: ImmutableSet<AuthLoader>,
    contract: AuthContract
) = BaseComposeScreen {
    Children(
        stack = stack,
        animation = stackAnimation(fade()),
    ) { child -> child.instance.component.Render() }
}
