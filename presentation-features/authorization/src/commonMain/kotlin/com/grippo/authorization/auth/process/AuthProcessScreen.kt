package com.grippo.authorization.auth.process

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation

@Composable
internal fun AuthProcessScreen(component: AuthProcessComponent) {
    Children(
        stack = component.childStack,
        animation = stackAnimation(fade()),
    ) { child -> child.instance.component.Render() }
}