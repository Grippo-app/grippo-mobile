package com.grippo.shared.root

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.grippo.core.BaseComposeScreen
import com.grippo.design.core.AppTokens
import com.grippo.presentation.api.RootRouter
import com.grippo.shared.root.RootComponent.Child
import kotlinx.collections.immutable.ImmutableSet

@Composable
public fun RootScreen(
    stack: Value<ChildStack<RootRouter, Child>>,
    state: RootState,
    contract: RootContract,
    loaders: ImmutableSet<RootLoader>,
): Unit = BaseComposeScreen {
    Children(
        modifier = Modifier.background(AppTokens.colors.background.primary),
        stack = stack,
        animation = stackAnimation(fade()),
        content = { child -> child.instance.component.Render() }
    )
}