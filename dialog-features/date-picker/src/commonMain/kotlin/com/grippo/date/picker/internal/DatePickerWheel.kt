package com.grippo.date.picker.internal

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
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.wheel.WheelItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.wheel.picker.DefaultSelectorProperties
import com.grippo.wheel.picker.MultiWheelPicker
import com.grippo.wheel.picker.WheelColumn
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month

@Composable
internal fun DateWheelPicker(
    modifier: Modifier = Modifier,
    initial: LocalDateTime,
    limitations: DateRange,
    select: (LocalDateTime) -> Unit,
) {
    var selectedYear by remember { mutableStateOf(initial.year) }
    var selectedMonth by remember { mutableStateOf(initial.month) }
    var selectedDay by remember { mutableStateOf(initial.dayOfMonth) }

    val years = remember(limitations) {
        (limitations.from.year..limitations.to.year).toList()
    }

    val months = remember { Month.entries }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        (1..DateTimeUtils.getDaysInMonth(selectedYear, selectedMonth)).toList()
    }

    val safeDay = remember(selectedDay, daysInMonth) {
        selectedDay.coerceIn(daysInMonth.first(), daysInMonth.last())
    }

    LaunchedEffect(selectedYear, selectedMonth, safeDay) {
        val localDate = runCatching {
            LocalDateTime(
                year = selectedYear,
                month = selectedMonth,
                dayOfMonth = safeDay,
                hour = initial.hour,
                minute = initial.minute
            )
        }.getOrNull()

        localDate
            ?.takeIf { it in limitations.from..limitations.to }
            ?.let(select)
    }

    val dayState = rememberLazyListState()
    val monthState = rememberLazyListState()
    val yearState = rememberLazyListState()

    val dayColumn = WheelColumn(
        id = "day",
        items = daysInMonth,
        selected = selectedDay,
        onSelect = { selectedDay = it },
        isValid = { day ->
            checkDayValidity(
                selectedYear,
                selectedMonth,
                day,
                limitations,
                initial.hour,
                initial.minute
            )
        },
        itemContent = { day, valid ->
            WheelItem(
                text = day.toString(),
                isValid = valid
            )
        },
        listState = dayState
    )

    val monthColumn = WheelColumn(
        id = "month",
        items = months,
        selected = selectedMonth,
        onSelect = { selectedMonth = it },
        isValid = { m -> checkMonthValidity(selectedYear, m, limitations) },
        itemContent = { m, valid ->
            WheelItem(
                text = m.name.lowercase().replaceFirstChar(Char::titlecase),
                isValid = valid
            )
        },
        listState = monthState
    )

    val yearColumn = WheelColumn(
        id = "year",
        items = years,
        selected = selectedYear,
        onSelect = { selectedYear = it },
        isValid = { y -> checkYearValidity(y, limitations) },
        itemContent = { y, valid ->
            WheelItem(
                text = y.toString(),
                isValid = valid
            )
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
        columns = listOf(dayColumn, monthColumn, yearColumn)
    )
}

private fun checkDayValidity(
    year: Int,
    month: Month,
    day: Int,
    limitations: DateRange,
    hour: Int,
    minute: Int,
): Boolean {
    if (!checkMonthValidity(year, month, limitations)) return false
    val dt =
        runCatching { LocalDateTime(year, month, day, hour, minute) }.getOrNull() ?: return false
    return dt in limitations.from..limitations.to
}

private fun checkMonthValidity(year: Int, month: Month, limitations: DateRange): Boolean {
    return when (year) {
        limitations.from.year -> month.ordinal >= limitations.from.month.ordinal
        limitations.to.year -> month.ordinal <= limitations.to.month.ordinal
        else -> true
    }
}

private fun checkYearValidity(year: Int, limitations: DateRange): Boolean {
    return year in limitations.from.year..limitations.to.year
}

@AppPreview
@Composable
private fun DateWheelPickerPreview() {
    PreviewContainer {
        DateWheelPicker(
            initial = DateTimeUtils.now(),
            limitations = DateTimeUtils.trailingYear(),
            select = {}
        )
    }
}