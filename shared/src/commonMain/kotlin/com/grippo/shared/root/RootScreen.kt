package com.grippo.shared.root

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandler
import com.grippo.core.BaseComposeScreen
import com.grippo.design.core.AppTokens
import com.grippo.platform.core.backAnimation
import com.grippo.presentation.api.RootRouter
import com.grippo.shared.root.RootComponent.Child
import kotlinx.collections.immutable.ImmutableSet
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
public fun RootScreen(
    stack: Value<ChildStack<RootRouter, Child>>,
    backHandler: BackHandler,
    state: RootState,
    contract: RootContract,
    loaders: ImmutableSet<RootLoader>,
): Unit = BaseComposeScreen {
    ChildStackCompose(
        modifier = Modifier.background(AppTokens.colors.background.primary),
        stack = stack,
        animation = backAnimation(
            backHandler = backHandler,
            onBack = contract::back
        ),
        content = { child -> child.instance.component.Render() }
    )
}