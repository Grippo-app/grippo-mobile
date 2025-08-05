package com.grippo.home.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.models.Side
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.components.tab.Tab
import com.grippo.design.components.tab.TabItem
import com.grippo.design.core.AppTokens
import com.grippo.design.core.UiText
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.Box
import com.grippo.home.BottomBarMenu
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun BottomNavigationMenu(
    items: ImmutableList<Pair<Int, TabItem>>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        content.invoke(this)

        Tab(
            modifier = Modifier
                .shadowDefault(
                    shape = RoundedCornerShape(0.dp),
                    elevation = ShadowElevation.Container,
                    sides = persistentListOf(Side.TOP)
                )
                .background(AppTokens.colors.background.dialog)
                .navigationBarsPadding()
                .fillMaxWidth(),
            items = items,
            selected = selected,
            onSelect = onSelect,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        BottomNavigationMenu(
            items = BottomBarMenu.entries
                .map {
                    it.ordinal to TabItem(
                        text = UiText.Str(it.name),
                        icon = AppTokens.icons.Box
                    )
                }
                .toPersistentList(),
            selected = 0,
            onSelect = { },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                )
            }
        )
    }
}