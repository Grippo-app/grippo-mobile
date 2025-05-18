package com.grippo.home

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.pages.ChildPages
import com.arkivanov.decompose.extensions.compose.pages.PagesScrollAnimation
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.grippo.core.BaseComposeScreen
import com.grippo.presentation.api.bottom.navigation.BottomNavigationRouter
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun BottomNavigationScreen(
    pages: Value<ChildPages<BottomNavigationRouter, BottomNavigationComponent.Child>>,
    state: BottomNavigationState,
    loaders: ImmutableSet<BottomNavigationLoader>,
    contract: BottomNavigationContract
) = BaseComposeScreen {
    ChildPages(
        pages = pages,
        onPageSelected = contract::selectPage,
        scrollAnimation = PagesScrollAnimation.Default,
        pageContent = { _, page -> page.component.Render() }
    )
}