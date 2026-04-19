package com.grippo.menu.picker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.menu.PickerMenuItem
import com.grippo.core.state.menu.TrainingMenu
import com.grippo.design.components.menu.Menu
import com.grippo.design.components.menu.MenuItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.menu
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun MenuPickerScreen(
    state: MenuPickerState,
    loaders: ImmutableSet<MenuPickerLoader>,
    contract: MenuPickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.menu),
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    val menuItems = state.items.map { item ->
        item to MenuItem(
            title = item.text(),
            icon = item.icon(),
            titleColor = item.textColor(),
            iconColor = item.iconColor()
        )
    }.toPersistentList()

    Menu(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        items = menuItems,
        onClick = contract::onItemClick,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        MenuPickerScreen(
            state = MenuPickerState(
                items = persistentListOf<PickerMenuItem>(
                    TrainingMenu.Details,
                    TrainingMenu.Edit,
                    TrainingMenu.Delete,
                ),
            ),
            loaders = persistentSetOf(),
            contract = MenuPickerContract.Empty
        )
    }
}
