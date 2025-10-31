package com.grippo.home.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.formatters.UiText
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.tab.Tab
import com.grippo.design.components.tab.TabItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.NavArrowDown
import com.grippo.design.resources.provider.start_workout
import com.grippo.home.BottomBarMenu
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun BottomNavigationMenu(
    items: ImmutableList<Pair<Int, TabItem>>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    onPrimaryButtonClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        content.invoke(this)

        Row(
            modifier = Modifier
                .background(AppTokens.colors.background.dialog)
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
                .navigationBarsPadding()
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val (leftItems, rightItems) = remember(items) {
                val mid = items.size / 2
                val left = items.subList(0, mid)
                val right = items.subList(mid, items.size)
                left to right
            }

            leftItems.forEach { item ->
                Tab(
                    modifier = Modifier.weight(1f),
                    item = item,
                    isSelected = selected == item.first,
                    onSelect = onSelect,
                )
            }

            Button(
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.start_workout)
                ),
                size = ButtonSize.Small,
                onClick = onPrimaryButtonClick
            )

            rightItems.forEach { item ->
                Tab(
                    modifier = Modifier.weight(1f),
                    item = item,
                    isSelected = selected == item.first,
                    onSelect = onSelect,
                )
            }
        }
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
                        icon = AppTokens.icons.NavArrowDown
                    )
                }
                .toPersistentList(),
            selected = 0,
            onSelect = { },
            onPrimaryButtonClick = {},
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