package com.grippo.date.picker.internal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.wheel.picker.DefaultSelectorProperties
import com.grippo.wheel.picker.MultiWheelPicker
import com.grippo.wheel.picker.WheelColumn
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.enums.EnumEntries

@Composable
internal fun DateWheelPicker(
    modifier: Modifier = Modifier,
    initial: LocalDateTime,
    limitations: Pair<LocalDateTime, LocalDateTime>,
    select: (LocalDateTime) -> Unit,
) {
    val months: EnumEntries<Month> = Month.entries
    val years: List<Int> = (limitations.first.year..limitations.second.year).toList()

    var selectedYear by remember { mutableStateOf(initial.year) }
    var selectedMonth by remember { mutableStateOf(initial.month) }
    var selectedDay by remember { mutableStateOf(initial.dayOfMonth) }

    val daysInMonth by remember(selectedYear, selectedMonth) {
        derivedStateOf { (1..getDaysInMonth(selectedYear, selectedMonth)).toList() }
    }

    val safeDay = remember(selectedDay, daysInMonth) {
        minOf(selectedDay, daysInMonth.last())
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
            ?.takeIf { it in limitations.first..limitations.second }
            ?.let(select)
    }

    MultiWheelPicker(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTokens.dp.wheelPicker.height),
        rowCount = 3,
        selectorProperties = DefaultSelectorProperties(
            enabled = true,
            shape = RoundedCornerShape(AppTokens.dp.wheelPicker.radius),
            color = AppTokens.colors.background.primary,
            border = BorderStroke(1.dp, AppTokens.colors.border.defaultPrimary)
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
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val start = now.date.minus(DatePeriod(years = 1)).atTime(now.time)
        val end = now.date.plus(DatePeriod(years = 1)).atTime(now.time)

        DateWheelPicker(
            initial = now,
            limitations = start to end,
            select = {}
        )
    }
}