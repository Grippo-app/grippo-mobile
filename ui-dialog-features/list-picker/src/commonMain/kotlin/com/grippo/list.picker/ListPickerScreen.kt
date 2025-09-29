package com.grippo.list.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.menu.Menu
import com.grippo.design.components.menu.MenuItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.formatters.UiText
import com.grippo.state.item.ItemState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ListPickerScreen(
    state: ListPickerState,
    loaders: ImmutableSet<ListPickerLoader>,
    contract: ListPickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.title,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        val menu = remember(state.items) {
            state.items
                .map { it.id to MenuItem(UiText.Str(it.text)) }
                .toPersistentList()
        }

        Menu(
            items = menu,
            onClick = contract::onItemClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ListPickerScreen(
            state = ListPickerState(
                title = "Select option",
                items = persistentListOf(
                    ItemState(id = "1", text = "First"),
                    ItemState(id = "2", text = "Second"),
                    ItemState(id = "3", text = "Third")
                ),
            ),
            loaders = persistentSetOf(),
            contract = ListPickerContract.Empty
        )
    }
}
