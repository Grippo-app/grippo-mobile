package com.grippo.period.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.BaseComposeDialog
import com.grippo.core.ScreenBackground
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.selectable.SelectableCard
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.components.datetime.DateRangeSelector
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.period_picker_title
import com.grippo.design.resources.submit_btn
import com.grippo.state.datetime.PeriodState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun PeriodPickerScreen(
    state: PeriodPickerState,
    loaders: ImmutableSet<PeriodPickerLoader>,
    contract: PeriodPickerContract
) = BaseComposeDialog(ScreenBackground.Color(AppTokens.colors.background.secondary)) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.period_picker_title),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        val list = remember(state.list) {
            state.list
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            items(
                items = list,
                key = { it.hashCode() },
                contentType = { it::class }
            ) { item ->
                val clickProvider = remember { { contract.onSelectClick(item) } }
                val isSelected = remember(state.initial) { state.initial == item }

                SelectableCard(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = isSelected,
                    onSelect = clickProvider,
                    style = SelectableCardStyle.Medium(
                        title = item.text(),
                        description = item.range(),
                        subContent = if (item is PeriodState.CUSTOM && isSelected) {
                            {
                                DateRangeSelector(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = item.range,
                                    onFromClick = contract::onFromClick,
                                    onToClick = contract::onToClick
                                )
                            }
                        } else {
                            null
                        }
                    )
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.submit_btn),
            style = ButtonStyle.Primary,
            onClick = contract::submit
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        PeriodPickerScreen(
            state = PeriodPickerState(
                initial = PeriodState.WEEKLY,
                list = persistentListOf(
                    PeriodState.DAILY,
                    PeriodState.WEEKLY,
                    PeriodState.MONTHLY,
                    PeriodState.CUSTOM(DateTimeUtils.thisDay()),
                )
            ),
            loaders = persistentSetOf(),
            contract = PeriodPickerContract.Empty
        )
    }
}