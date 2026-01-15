package com.grippo.month.picker.internal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.grippo.design.components.wheel.WheelItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateCompose.rememberFormat
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.wheel.picker.DefaultSelectorProperties
import com.grippo.wheel.picker.MultiWheelPicker
import com.grippo.wheel.picker.WheelColumn
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month

@Composable
internal fun MonthWheelPicker(
    modifier: Modifier = Modifier,
    initial: LocalDateTime?,
    limitations: DateRange,
    select: (LocalDateTime) -> Unit,
) {

    val resolvedInitial: LocalDateTime = remember(initial, limitations) {
        when {
            initial == null -> limitations.from
            initial < limitations.from -> limitations.from
            initial > limitations.to -> limitations.to
            else -> initial
        }
    }

    val baseHour = remember(resolvedInitial) { resolvedInitial.hour }
    val baseMinute = remember(resolvedInitial) { resolvedInitial.minute }

    var selectedYear by remember { mutableStateOf(resolvedInitial.year) }
    var selectedMonth by remember { mutableStateOf(resolvedInitial.month) }

    val years = remember(limitations) {
        (limitations.from.year..limitations.to.year).toList()
    }
    val months = remember { Month.entries }

    LaunchedEffect(selectedYear) {
        val clamped = clampMonthToBounds(selectedYear, selectedMonth, limitations)
        if (clamped != selectedMonth) {
            selectedMonth = clamped
        }
    }

    LaunchedEffect(selectedYear, selectedMonth) {
        val clamped = clampMonthToBounds(selectedYear, selectedMonth, limitations)
        if (clamped != selectedMonth) {
            selectedMonth = clamped
            return@LaunchedEffect
        }

        val dayRange = validDayRange(selectedYear, selectedMonth, limitations)
        val day = dayRange.first
        val dt = runCatching {
            LocalDateTime(
                year = selectedYear,
                month = selectedMonth,
                day = day,
                hour = baseHour,
                minute = baseMinute
            )
        }.getOrNull()

        dt?.takeIf { it in limitations.from..limitations.to }?.let(select)
    }

    val monthState = rememberLazyListState()
    val yearState = rememberLazyListState()

    val monthColumn = WheelColumn(
        id = "month",
        items = months,
        selected = selectedMonth,
        onSelect = { selectedMonth = it },
        isValid = { m -> checkMonthValidity(selectedYear, m, limitations) },
        itemContent = { month, valid ->
            val template = remember(month, selectedYear) {
                LocalDateTime(selectedYear, month, 1, 0, 0)
            }
            val monthName = rememberFormat(value = template, format = DateFormat.DateOnly.MonthFull)
            WheelItem(text = monthName, isValid = valid)
        },
        listState = monthState
    )

    val yearColumn = WheelColumn(
        id = "year",
        items = years,
        selected = selectedYear,
        onSelect = { selectedYear = it },
        isValid = { year -> checkYearValidity(year, limitations) },
        itemContent = { year, valid ->
            WheelItem(text = year.toString(), isValid = valid)
        },
        listState = yearState
    )

    MultiWheelPicker(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTokens.dp.wheelPicker.height),
        selectorProperties = DefaultSelectorProperties(
            shape = RoundedCornerShape(AppTokens.dp.wheelPicker.radius),
            color = AppTokens.colors.background.card,
        ),
        columns = listOf(monthColumn, yearColumn)
    )
}

private fun monthBounds(year: Int, limits: DateRange): Pair<Month, Month> {
    val min = if (year == limits.from.year) limits.from.month else Month.JANUARY
    val max = if (year == limits.to.year) limits.to.month else Month.DECEMBER
    return min to max
}

private fun clampMonthToBounds(year: Int, month: Month, limits: DateRange): Month {
    val (minM, maxM) = monthBounds(year, limits)
    return when {
        month.ordinal < minM.ordinal -> minM
        month.ordinal > maxM.ordinal -> maxM
        else -> month
    }
}

private fun validDayRange(year: Int, month: Month, limits: DateRange): IntRange {
    val maxDays = DateTimeUtils.getDaysInMonth(year, month)
    val minDay = if (year == limits.from.year && month == limits.from.month)
        limits.from.day else 1
    val maxDay = if (year == limits.to.year && month == limits.to.month)
        minOf(limits.to.day, maxDays) else maxDays
    val start = minDay.coerceAtMost(maxDay)
    return start..maxDay
}

private fun checkMonthValidity(year: Int, month: Month, limitations: DateRange): Boolean {
    val (minM, maxM) = monthBounds(year, limitations)
    return month.ordinal in minM.ordinal..maxM.ordinal
}

private fun checkYearValidity(year: Int, limitations: DateRange): Boolean {
    return year in limitations.from.year..limitations.to.year
}

@AppPreview
@Composable
private fun MonthWheelPickerPreview() {
    PreviewContainer {
        MonthWheelPicker(
            initial = DateTimeUtils.now(),
            limitations = DateRange.Range.Last365Days().range,
            select = {}
        )
    }
}
