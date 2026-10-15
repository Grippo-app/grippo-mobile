package com.grippo.date.picker.internal

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.design.components.frames.FocusFrame
import com.grippo.design.components.wheel.WheelItemRow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateCompose.rememberFormat
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.wheel.picker.DefaultSelectorProperties
import com.grippo.wheel.picker.MultiWheelPicker
import com.grippo.wheel.picker.MultiWheelPickerConfig
import com.grippo.wheel.picker.WheelColumn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month

@Composable
internal fun DateWheelPicker(
    modifier: Modifier = Modifier,
    initial: LocalDateTime?,
    limitations: DateRange,
    select: (LocalDateTime) -> Unit,
) {

    // Resolve initial date-time safely: if null -> use earliest allowed (limitations.from).
    // Also clamp any out-of-range initial to bounds.
    val value: LocalDateTime = remember(initial, limitations) {
        when {
            initial == null -> limitations.from
            initial < limitations.from -> limitations.from
            initial > limitations.to -> limitations.to
            else -> initial
        }
    }

    val time = value.time

    var selectedYear by remember { mutableStateOf(value.year) }
    var selectedMonth by remember { mutableStateOf(value.month) }
    var selectedDay by remember { mutableStateOf(value.day) }

    val years = remember(limitations) {
        (limitations.from.year..limitations.to.year).toList()
    }
    val months = remember { Month.entries }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        (1..DateTimeUtils.getDaysInMonth(selectedYear, selectedMonth)).toList()
    }

    // Keep selectedDay within actual month length first (e.g., 31 -> Feb)
    LaunchedEffect(daysInMonth) {
        if (selectedDay !in daysInMonth) {
            selectedDay = daysInMonth.last()
        }
    }

    // Year changed: clamp month to allowed bounds for that year, then clamp day to allowed range.
    LaunchedEffect(selectedYear) {
        val clampedMonth = clampMonthToBounds(selectedYear, selectedMonth, limitations)
        if (clampedMonth != selectedMonth) {
            selectedMonth = clampedMonth
        }
        val dayRange = validDayRange(selectedYear, clampedMonth, limitations)
        if (selectedDay !in dayRange) {
            selectedDay = nearestInRange(selectedDay, dayRange)
        }
    }

    // Month changed: if month is out of bounds for current year, snap to nearest allowed month.
    // Then clamp the day to valid range for that (year, month).
    LaunchedEffect(selectedMonth, selectedYear) {
        val clampedMonth = clampMonthToBounds(selectedYear, selectedMonth, limitations)
        if (clampedMonth != selectedMonth) {
            selectedMonth = clampedMonth
            return@LaunchedEffect // next effect run will handle days
        }
        val dayRange = validDayRange(selectedYear, selectedMonth, limitations)
        if (selectedDay !in dayRange) {
            selectedDay = nearestInRange(selectedDay, dayRange)
        }
    }

    // Date selection keeps the complete time, including precision at range bounds.
    LaunchedEffect(selectedYear, selectedMonth, selectedDay, daysInMonth, time, limitations) {
        val safeDay = selectedDay.coerceIn(daysInMonth.first(), daysInMonth.last())
        val dt = runCatching {
            LocalDateTime(
                date = LocalDate(selectedYear, selectedMonth, safeDay),
                time = time,
            )
        }.getOrNull()

        dt?.takeIf { it in limitations.from..limitations.to }?.let(select)
    }

    val dayState = rememberLazyListState(
        initialFirstVisibleItemIndex = daysInMonth.indexOf(selectedDay).coerceAtLeast(0),
    )
    val monthState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedMonth.ordinal,
    )
    val yearState = rememberLazyListState(
        initialFirstVisibleItemIndex = years.indexOf(selectedYear).coerceAtLeast(0),
    )

    val dayColumn = WheelColumn(
        id = "day",
        items = daysInMonth,
        selected = selectedDay,
        onSelect = { selectedDay = it },
        isValid = { d ->
            checkDayValidity(
                year = selectedYear,
                month = selectedMonth,
                day = d,
                limitations = limitations,
                time = time,
            )
        },
        itemContent = { d, valid ->
            WheelItemRow(text = d.toString(), isValid = valid)
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
            val template = remember(m) {
                LocalDateTime(selectedYear, m.ordinal + 1, 1, 0, 0)
            }
            val monthName = rememberFormat(value = template, format = DateFormat.DateOnly.MonthFull)
            WheelItemRow(text = monthName, isValid = valid)
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
            WheelItemRow(text = y.toString(), isValid = valid)
        },
        listState = yearState
    )

    val config = MultiWheelPickerConfig()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTokens.dp.wheelPicker.height),
    ) {
        FocusFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(maxHeight / config.rowCount),
            content = {},
        )

        MultiWheelPicker(
            modifier = Modifier.fillMaxSize(),
            selectorProperties = DefaultSelectorProperties(
                shape = RoundedCornerShape(AppTokens.dp.wheelPicker.radius),
                color = Color.Transparent,
            ),
            config = config,
            columns = listOf(dayColumn, monthColumn, yearColumn),
        )
    }
}

// Inclusive allowed month bounds for the given year
private fun monthBounds(year: Int, limits: DateRange): Pair<Month, Month> {
    val min = if (year == limits.from.year) limits.from.month else Month.JANUARY
    val max = if (year == limits.to.year) limits.to.month else Month.DECEMBER
    return min to max
}

// Clamp month into allowed bounds for the given year
private fun clampMonthToBounds(year: Int, month: Month, limits: DateRange): Month {
    val (minM, maxM) = monthBounds(year, limits)
    return when {
        month.ordinal < minM.ordinal -> minM
        month.ordinal > maxM.ordinal -> maxM
        else -> month
    }
}

// Valid day window for (year, month) considering edge months of the limits.
// If both edges are the same month, it becomes from.day..to.day.
private fun validDayRange(year: Int, month: Month, limits: DateRange): IntRange {
    val maxDays = DateTimeUtils.getDaysInMonth(year, month)
    val minDay = if (year == limits.from.year && month == limits.from.month)
        limits.from.day else 1
    val maxDay = if (year == limits.to.year && month == limits.to.month)
        minOf(limits.to.day, maxDays) else maxDays
    return minDay.coerceAtMost(maxDay)..maxDay
}

private fun nearestInRange(value: Int, range: IntRange): Int =
    value.coerceIn(range.first, range.last)

private fun checkDayValidity(
    year: Int,
    month: Month,
    day: Int,
    limitations: DateRange,
    time: LocalTime,
): Boolean {
    if (!checkMonthValidity(year, month, limitations)) return false
    val dt =
        runCatching { LocalDateTime(LocalDate(year, month, day), time) }.getOrNull() ?: return false
    return dt in limitations.from..limitations.to
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
private fun DateWheelPickerPreview() {
    PreviewContainer {
        DateWheelPicker(
            initial = LocalDateTime(2026, 9, 18, 12, 0),
            limitations = DateRange(
                from = LocalDateTime(2025, 9, 5, 0, 0),
                to = LocalDateTime(2026, 10, 21, 23, 59),
            ),
            select = {}
        )
    }
}
