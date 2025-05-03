package com.grippo.shared.root

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation

@Composable
public fun RootScreen(component: RootComponent) {
    Children(
        modifier = Modifier.systemBarsPadding(),
        stack = component.childStack,
        animation = stackAnimation(fade()),
    ) { child -> child.instance.component.Render() }
}