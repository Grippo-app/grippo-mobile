package com.grippo.home.bottom.navigation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.grippo.core.BaseComposeScreen
import com.grippo.presentation.api.bottom.navigation.BottomNavigationRouter
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun BottomNavigationScreen(
    page: Value<ChildPages<BottomNavigationRouter>>
    state: BottomNavigationState,
    loaders: ImmutableSet<BottomNavigationLoader>,
    contract: BottomNavigationContract
) = BaseComposeScreen {
    Children(
        stack = stack,
        animation = stackAnimation(fade()),
    ) { child -> child.instance.component.Render() }
}