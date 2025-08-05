package com.grippo.date.picker.internal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.DateTimeUtils
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

    val months = remember(selectedYear, limitations) {
        val allMonths = Month.entries
        when (selectedYear) {
            limitations.from.year -> allMonths.slice(limitations.from.month.ordinal..11)
            limitations.to.year -> allMonths.slice(0..limitations.to.month.ordinal)
            else -> allMonths
        }
    }

    val daysInMonth = remember(selectedYear, selectedMonth, limitations) {
        val maxDay = getDaysInMonth(selectedYear, selectedMonth)
        val minDay =
            if (selectedYear == limitations.from.year && selectedMonth == limitations.from.month)
                limitations.from.dayOfMonth else 1
        val maxValidDay =
            if (selectedYear == limitations.to.year && selectedMonth == limitations.to.month)
                minOf(limitations.to.dayOfMonth, maxDay) else maxDay
        (minDay..maxValidDay).toList()
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
                itemContent = {
                    Text(
                        text = it.toString(),
                        style = AppTokens.typography.b16Bold(),
                        color = AppTokens.colors.text.primary
                    )
                }
            ),
            WheelColumn(
                id = "month",
                items = months,
                initial = selectedMonth,
                onValueChange = { selectedMonth = it },
                itemContent = {
                    Text(
                        text = it.name.lowercase().replaceFirstChar(Char::titlecase),
                        style = AppTokens.typography.b16Bold(),
                        color = AppTokens.colors.text.primary
                    )
                }
            ),
            WheelColumn(
                id = "year",
                items = years,
                initial = selectedYear,
                onValueChange = { selectedYear = it },
                itemContent = {
                    Text(
                        text = it.toString(),
                        style = AppTokens.typography.b16Bold(),
                        color = AppTokens.colors.text.primary
                    )
                }
            )
        )
    )
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