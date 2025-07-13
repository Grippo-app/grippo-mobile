package com.grippo.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.tab.TabItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.home.internal.BottomNavigationMenu
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentList
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack as ChildStackCompose

@Composable
internal fun BottomNavigationScreen(
    component: BottomNavigationComponent,
    state: BottomNavigationState,
    loaders: ImmutableSet<BottomNavigationLoader>,
    contract: BottomNavigationContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.primary)) {

    val tabs = BottomBarMenu.entries
        .map { it.ordinal to TabItem(text = it.title(), icon = it.icon()) }
        .toPersistentList()

    BottomNavigationMenu(
        items = tabs,
        selected = state.selected.ordinal,
        onSelect = contract::selectTab,
        content = {
            ChildStackCompose(
                modifier = Modifier.fillMaxWidth().weight(1f),
                stack = component.childStack,
                animation = stackAnimation(animator = fade()),
                content = { child -> child.instance.component.Render() }
            )
        }
    )
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        BottomNavigationMenu(
            items = BottomBarMenu.entries
                .map { it.ordinal to TabItem(text = it.title(), icon = it.icon()) }
                .toPersistentList(),
            selected = 0,
            onSelect = { },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(AppTokens.colors.background.primary)
                )
            }
        )
    }
}