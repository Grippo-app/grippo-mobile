package com.grippo.period.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.design.components.cards.selectable.CheckSelectableCard
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.toolbar.BottomSheetToolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.custom
import com.grippo.design.resources.provider.period_picker_title
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangeKind
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDateTime

@Composable
internal fun PeriodPickerScreen(
    state: PeriodPickerState,
    loaders: ImmutableSet<PeriodPickerLoader>,
    contract: PeriodPickerContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        contentPadding = PaddingValues(
            start = AppTokens.dp.dialog.horizontalPadding,
            end = AppTokens.dp.dialog.horizontalPadding,
            top = AppTokens.dp.dialog.top,
            bottom = AppTokens.dp.dialog.bottom,
        ),
    ) {
        items(state.suggestions, key = { it.kind.name }) { item ->
            CheckSelectableCard(
                modifier = Modifier.fillMaxWidth(),
                style = CheckSelectableCardStyle.Medium(
                    title = item.label()?.text() ?: AppTokens.strings.res(Res.string.custom),
                    description = item.display,
                ),
                isSelected = state.value.kind == item.kind,
                onSelect = { contract.onSelectRange(item.kind) }
            )
        }
    }
}

@AppPreview
@Composable
private fun PeriodPickerSelectedPreview() {
    PreviewContainer {
        PeriodPickerSheetPreview(periodPickerPreviewState(DateRangeKind.Last30Days))
    }
}

@AppPreview
@Composable
private fun PeriodPickerUnmatchedSelectionPreview() {
    PreviewContainer {
        PeriodPickerSheetPreview(periodPickerPreviewState(DateRangeKind.Weekly))
    }
}

@Composable
private fun PeriodPickerSheetPreview(state: PeriodPickerState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = AppTokens.dp.bottomSheet.radius,
                    topEnd = AppTokens.dp.bottomSheet.radius,
                )
            )
            .background(AppTokens.colors.background.dialog)
    ) {
        BottomSheetToolbar(
            title = AppTokens.strings.res(Res.string.period_picker_title),
            allowBack = false,
            onBack = {},
            onClose = {},
        )
        PeriodPickerScreen(
            state = state,
            loaders = persistentSetOf(),
            contract = PeriodPickerContract.Empty,
        )
    }
}

private fun periodPickerPreviewState(selected: DateRangeKind): PeriodPickerState {
    val to = LocalDateTime(2026, 1, 5, 23, 59, 59)
    val suggestions = persistentListOf(
        DateRangeKind.Last7Days to LocalDateTime(2025, 12, 30, 0, 0),
        DateRangeKind.Last14Days to LocalDateTime(2025, 12, 23, 0, 0),
        DateRangeKind.Last30Days to LocalDateTime(2025, 12, 7, 0, 0),
        DateRangeKind.Last60Days to LocalDateTime(2025, 11, 7, 0, 0),
    ).map { (kind, from) ->
        val fromLabel = DateTimeUtils.format(from, DateFormat.DateOnly.DateDdMmm)
        val toLabel = DateTimeUtils.format(to, DateFormat.DateOnly.DateDdMmm)
        DateRangeFormatState.Valid(
            display = "$fromLabel - $toLabel",
            value = DateRange(from, to),
            kind = kind,
        )
    }.toImmutableList()
    return PeriodPickerState(
        value = suggestions.firstOrNull { it.kind == selected }
            ?: DateRangeFormatState.Empty(kind = selected),
        suggestions = suggestions,
    )
}
