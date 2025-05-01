package com.grippo.authorization

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation

@Composable
internal fun AuthScreen(component: AuthComponent) {
    Children(
        stack = component.childStack,
        animation = stackAnimation(fade()),
    ) { child ->
        child.instance.component.Render()
    }
}
