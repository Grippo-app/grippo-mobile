package com.grippo.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.UiText
import com.grippo.design.components.navigation.BottomBar
import com.grippo.design.components.navigation.BottomBarItem
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.calendar
import com.grippo.design.resources.provider.home
import com.grippo.design.resources.provider.icons.Calendar
import com.grippo.design.resources.provider.icons.Home
import com.grippo.design.resources.provider.icons.UserOutline
import com.grippo.design.resources.provider.profile
import com.grippo.screen.api.MainRouter
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun MainScreen(
    component: MainComponent,
    state: MainState,
    loaders: ImmutableSet<MainLoader>,
    contract: MainContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    val stack by component.childStack.subscribeAsState()
    val icons = AppTokens.icons
    val items = remember(icons) {
        persistentListOf(
            MainRouter.Home to BottomBarItem(UiText.Res(Res.string.home), icons.Home),
            MainRouter.Calendar to BottomBarItem(UiText.Res(Res.string.calendar), icons.Calendar),
            MainRouter.Profile to BottomBarItem(UiText.Res(Res.string.profile), icons.UserOutline),
        )
    }
    Box(
        modifier = Modifier.fillMaxWidth().weight(1f)
            .consumeWindowInsets(WindowInsets.navigationBars),
    ) {
        ChildStackCompose(
            modifier = Modifier.fillMaxSize(),
            stack = component.childStack,
            animation = stackAnimation(animator = fade()),
            content = { child -> child.instance.Render() },
        )
    }
    BottomBar(
        modifier = Modifier.fillMaxWidth(),
        items = items,
        selected = stack.active.configuration,
        onSelect = contract::onSelectTab,
    )
}
