package com.grippo.authorization.registration

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.grippo.authorization.registration.RegistrationComponent.Child
import com.grippo.core.BaseComposeScreen
import com.grippo.presentation.api.auth.RegistrationRouter
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun RegistrationScreen(
    stack: Value<ChildStack<RegistrationRouter, Child>>,
    state: RegistrationState,
    loaders: ImmutableSet<RegistrationLoader>,
    contract: RegistrationContract
) = BaseComposeScreen {
    Children(
        stack = stack,
        animation = stackAnimation(fade()),
    ) { child -> child.instance.component.Render() }
}