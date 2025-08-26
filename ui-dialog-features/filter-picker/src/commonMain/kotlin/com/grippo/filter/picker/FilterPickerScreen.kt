package com.grippo.filter.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.selectable.SelectableCard
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.filters
import com.grippo.design.resources.provider.submit_btn
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun FilterPickerScreen(
    state: FilterPickerState,
    loaders: ImmutableSet<FilterPickerLoader>,
    contract: FilterPickerContract
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
            text = AppTokens.strings.res(Res.string.filters),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            SelectableCard(
                style = SelectableCardStyle.Small(
                    title = "Test"
                ),
                isSelected = true,
                onSelect = {}
            )

            SelectableCard(
                style = SelectableCardStyle.Small(
                    title = "Test two"
                ),
                isSelected = true,
                onSelect = {}
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            items(
                items = state.list,
                key = { it.hashCode() },
                contentType = { it::class }
            ) { item ->

            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier.fillMaxWidth(),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.submit_btn),
            ),
            style = ButtonStyle.Primary,
            onClick = contract::onSubmitClick
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        FilterPickerScreen(
            state = FilterPickerState(
                list = persistentListOf()
            ),
            loaders = persistentSetOf(),
            contract = FilterPickerContract.Empty
        )
    }
}