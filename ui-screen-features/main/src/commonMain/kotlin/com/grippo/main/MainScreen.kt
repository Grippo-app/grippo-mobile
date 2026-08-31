package com.grippo.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun MainScreen(
    component: MainComponent,
    state: MainState,
    loaders: ImmutableSet<MainLoader>,
    contract: MainContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    val pages by component.childPages.subscribeAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        pages.items
            .getOrNull(pages.selectedIndex)
            ?.instance
            ?.component
            ?.Render()
    }
}
