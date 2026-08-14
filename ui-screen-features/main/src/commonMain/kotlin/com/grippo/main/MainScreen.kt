package com.grippo.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.bottombar.BottomBar
import com.grippo.design.components.bottombar.BottomBarItem
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.calendar
import com.grippo.design.resources.provider.exercises
import com.grippo.design.resources.provider.home
import com.grippo.design.resources.provider.icons.Gauge
import com.grippo.design.resources.provider.icons.Muscle
import com.grippo.design.resources.provider.icons.Timer
import com.grippo.design.resources.provider.icons.User
import com.grippo.design.resources.provider.profile
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun MainScreen(
    component: MainComponent,
    state: MainState,
    loaders: ImmutableSet<MainLoader>,
    contract: MainContract,
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen
    )
) {
    val pages by component.childPages.subscribeAsState()
    val selectedIndex = pages.selectedIndex

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        pages.items
            .getOrNull(selectedIndex)
            ?.instance
            ?.component
            ?.Render()
    }

    val items = persistentListOf(
        BottomBarItem(
            label = AppTokens.strings.res(Res.string.home),
            icon = AppTokens.icons.Gauge,
            selected = selectedIndex == 0,
            onClick = { contract.onTabSelected(0) },
        ),
        BottomBarItem(
            label = AppTokens.strings.res(Res.string.calendar),
            icon = AppTokens.icons.Timer,
            selected = selectedIndex == 1,
            onClick = { contract.onTabSelected(1) },
        ),
        BottomBarItem(
            label = AppTokens.strings.res(Res.string.exercises),
            icon = AppTokens.icons.Muscle,
            selected = selectedIndex == 2,
            onClick = { contract.onTabSelected(2) },
        ),
        BottomBarItem(
            label = AppTokens.strings.res(Res.string.profile),
            icon = AppTokens.icons.User,
            selected = selectedIndex == 3,
            onClick = { contract.onTabSelected(3) },
        ),
    )

    BottomBar(
        items = items,
        onActionClick = contract::onStartTraining,
    )
}
