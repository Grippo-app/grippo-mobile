package com.grippo.date.picker.internal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.minus

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
        (1..getDaysInMonth(selectedYear, selectedMonth)).toList()
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

    MultiWheelPicker(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTokens.dp.wheelPicker.height),
        selectorProperties = DefaultSelectorProperties(
            shape = RoundedCornerShape(AppTokens.dp.wheelPicker.radius),
            color = AppTokens.colors.background.card,
        ),
        columns = listOf(
            WheelColumn(
                id = "day",
                items = daysInMonth,
                initial = selectedDay,
                onValueChange = { selectedDay = it },
                itemContent = { day ->
                    WheelItem(
                        text = day.toString(),
                        isValid = checkDayValidity(
                            year = selectedYear,
                            month = selectedMonth,
                            day = day,
                            limitations = limitations,
                            hour = initial.hour,
                            minute = initial.minute
                        )
                    )
                },
                onFinish = { idx ->
                    nearestValidIndex(
                        items = daysInMonth,
                        fromIndex = idx,
                        isValid = { day ->
                            checkDayValidity(
                                year = selectedYear,
                                month = selectedMonth,
                                day = day,
                                limitations = limitations,
                                hour = initial.hour,
                                minute = initial.minute
                            )
                        }
                    )
                }
            ),
            WheelColumn(
                id = "month",
                items = months,
                initial = selectedMonth,
                onValueChange = { selectedMonth = it },
                itemContent = { month ->
                    WheelItem(
                        text = month.name.lowercase().replaceFirstChar(Char::titlecase),
                        isValid = checkMonthValidity(
                            year = selectedYear,
                            month = month,
                            limitations = limitations
                        )
                    )
                },
                onFinish = { idx ->
                    nearestValidIndex(
                        items = months,
                        fromIndex = idx,
                        isValid = { month ->
                            checkMonthValidity(
                                year = selectedYear,
                                month = month,
                                limitations = limitations
                            )
                        }
                    )
                }
            ),
            WheelColumn(
                id = "year",
                items = years,
                initial = selectedYear,
                onValueChange = { selectedYear = it },
                itemContent = { year ->
                    WheelItem(
                        text = year.toString(),
                        isValid = checkYearValidity(
                            year = year,
                            limitations = limitations
                        )
                    )
                },
                onFinish = { idx ->
                    nearestValidIndex(
                        items = years,
                        fromIndex = idx,
                        isValid = { year ->
                            checkYearValidity(
                                year = year,
                                limitations = limitations
                            )
                        }
                    )
                }
            )
        )
    )
}

private fun <T> nearestValidIndex(
    items: List<T>,
    fromIndex: Int,
    isValid: (T) -> Boolean
): Int? {
    if (fromIndex !in items.indices) return null
    if (isValid(items[fromIndex])) return null
    var left = fromIndex - 1
    var right = fromIndex + 1
    while (left >= 0 || right < items.size) {
        if (left >= 0 && isValid(items[left])) return left
        if (right < items.size && isValid(items[right])) return right
        left--; right++
    }
    return null
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

private fun getDaysInMonth(year: Int, month: Month): Int {
    val nextMonth = if (month == Month.DECEMBER) Month.JANUARY else Month.entries[month.ordinal + 1]
    val nextYear = if (month == Month.DECEMBER) year + 1 else year
    val firstOfNextMonth = LocalDate(nextYear, nextMonth, 1)
    val lastDayOfThisMonth = firstOfNextMonth.minus(DatePeriod(days = 1))
    return lastDayOfThisMonth.dayOfMonth
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