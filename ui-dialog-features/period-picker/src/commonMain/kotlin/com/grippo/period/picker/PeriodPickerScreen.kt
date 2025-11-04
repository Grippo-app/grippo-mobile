package com.grippo.period.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.datetime.PeriodState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.cards.selectable.SelectableCard
import com.grippo.design.components.datetime.DateRangeSelector
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.period_picker_title
import com.grippo.design.resources.provider.submit_btn
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun PeriodPickerScreen(
    state: PeriodPickerState,
    loaders: ImmutableSet<PeriodPickerLoader>,
    contract: PeriodPickerContract
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
            text = AppTokens.strings.res(Res.string.period_picker_title),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            items(
                items = state.list,
                key = { it.hashCode() },
            ) { item ->
                val clickProvider = remember(item) { { contract.onSelectClick(item) } }
                val isSelected = remember(state.value, item) { state.value == item }

                SelectableCard(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = isSelected,
                    onSelect = clickProvider,
                    style = CheckSelectableCardStyle.Large(
                        title = item.title(),
                        description = item.range(DateFormat.DATE_MMM_DD_YYYY),
                        icon = item.icon(),
                        subContent = if (item is PeriodState.Custom) {
                            {
                                DateRangeSelector(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = item.range,
                                    enabled = isSelected,
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

        val hasCustomPicker = remember(state.list) {
            state.list.any { it is PeriodState.Custom }
        }

        if (hasCustomPicker) {

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier.fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.submit_btn),
                ),
                style = ButtonStyle.Primary,
                onClick = contract::onSubmitClick
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        PeriodPickerScreen(
            state = PeriodPickerState(
                value = PeriodState.ThisWeek,
                list = persistentListOf(
                    PeriodState.ThisDay,
                    PeriodState.ThisWeek,
                    PeriodState.ThisMonth,
                    PeriodState.Custom(
                        range = DateTimeUtils.thisWeek(),
                        limitations = DateTimeUtils.trailingYear()
                    ),
                ),
            ),
            loaders = persistentSetOf(),
            contract = PeriodPickerContract.Empty
        )
    }
}
