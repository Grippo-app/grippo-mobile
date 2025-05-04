package com.grippo.authorization.auth.process

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.grippo.authorization.auth.process.AuthProcessComponent.Child
import com.grippo.presentation.api.auth.AuthProcessRouter

@Composable
internal fun AuthProcessScreen(stack: Value<ChildStack<AuthProcessRouter, Child>>) {
    Children(
        stack = stack,
        animation = stackAnimation(fade()),
    ) { child -> child.instance.component.Render() }
}