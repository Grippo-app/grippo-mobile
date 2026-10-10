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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.UiText
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.navigation.BottomBar
import com.grippo.design.components.navigation.BottomBarItem
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.calendar
import com.grippo.design.resources.provider.home
import com.grippo.design.resources.provider.icons.Calendar
import com.grippo.design.resources.provider.icons.Home
import com.grippo.design.resources.provider.icons.Play
import com.grippo.design.resources.provider.icons.UserOutline
import com.grippo.design.resources.provider.profile
import com.grippo.design.resources.provider.start_workout
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
    val actionLift = AppTokens.dp.bottomBar.actionLift
    Layout(
        modifier = Modifier.fillMaxWidth().weight(1f),
        content = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .consumeWindowInsets(WindowInsets.navigationBars)
                    .clipToBounds(),
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
                action = {
                    val label = AppTokens.strings.res(Res.string.start_workout)
                    Button(
                        modifier = Modifier.fillMaxSize().semantics { contentDescription = label },
                        content = ButtonContent.Icon(ButtonIcon.Icon(AppTokens.icons.Play)),
                        state = if (MainLoader.StartTraining in loaders) ButtonState.Loading else ButtonState.Enabled,
                        onClick = contract::onStartTraining,
                    )
                },
            )
        },
    ) { measurables, constraints ->
        val bottomBar = measurables[1].measure(constraints.copy(minHeight = 0))
        val contentHeight = (constraints.maxHeight - bottomBar.height +
            actionLift.roundToPx()).coerceAtLeast(0)
        val content = measurables[0].measure(
            constraints.copy(minHeight = contentHeight, maxHeight = contentHeight),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            content.placeRelative(0, 0)
            bottomBar.placeRelative(0, constraints.maxHeight - bottomBar.height)
        }
    }
}
