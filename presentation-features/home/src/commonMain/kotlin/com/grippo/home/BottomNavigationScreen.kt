package com.grippo.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.pages.ChildPages
import com.arkivanov.decompose.extensions.compose.pages.PagesScrollAnimation
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.tab.TabItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.home.internal.BottomNavigationMenu
import com.grippo.presentation.api.bottom.navigation.BottomNavigationRouter
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun BottomNavigationScreen(
    pages: Value<ChildPages<BottomNavigationRouter, BottomNavigationComponent.Child>>,
    state: BottomNavigationState,
    loaders: ImmutableSet<BottomNavigationLoader>,
    contract: BottomNavigationContract
) = BaseComposeScreen(AppTokens.colors.background.primary) {

    val tabs = BottomBarMenu.entries
        .map { it.ordinal to TabItem(text = it.title(), icon = it.icon()) }
        .toPersistentList()

    BottomNavigationMenu(
        items = tabs,
        selected = state.selectedIndex,
        onSelect = contract::selectPage,
        content = {
            ChildPages(
                modifier = Modifier.fillMaxWidth().weight(1f),
                pages = pages,
                onPageSelected = contract::selectPage,
                scrollAnimation = PagesScrollAnimation.Default,
                pageContent = { _, page -> page.component.Render() }
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