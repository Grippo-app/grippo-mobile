package com.grippo.duration.picker.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.design.components.wheel.WheelItemRow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.hours_short
import com.grippo.design.resources.provider.minutes_short
import com.grippo.duration.picker.DefaultDurationHours
import com.grippo.duration.picker.DefaultDurationMinutes
import com.grippo.wheel.picker.DefaultSelectorProperties
import com.grippo.wheel.picker.MultiWheelPicker
import com.grippo.wheel.picker.WheelColumn
import kotlinx.collections.immutable.PersistentList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun DurationWheelPicker(
    modifier: Modifier = Modifier,
    hours: PersistentList<Int>,
    minutes: PersistentList<Int>,
    value: DurationFormatState,
    select: (Duration) -> Unit
) {
    val hoursState = rememberLazyListState()
    val minutesState = rememberLazyListState()

    val hourLabel = AppTokens.strings.res(Res.string.hours_short)
    val minuteLabel = AppTokens.strings.res(Res.string.minutes_short)

    val durationValue = remember(value) {
        value.value ?: DurationFormatState.DurationLimitation.start
    }

    val minTotalMinutes = remember {
        DurationFormatState.DurationLimitation.start.inWholeMinutes.toInt()
    }
    val maxTotalMinutes = remember {
        DurationFormatState.DurationLimitation.endInclusive.inWholeMinutes.toInt()
    }
    val minHours = remember(hours) { hours.firstOrNull() ?: 0 }
    val maxHours = remember(hours) { hours.lastOrNull() ?: 0 }

    fun validMinutesRangeForHour(hour: Int): IntRange {
        val minMinute = if (hour == minHours) {
            (minTotalMinutes - hour * 60).coerceAtLeast(0)
        } else 0
        val maxMinute = if (hour == maxHours) {
            (maxTotalMinutes - hour * 60).coerceAtMost(59)
        } else 59
        return minMinute.coerceAtMost(maxMinute)..maxMinute
    }

    val resolvedHours = remember(durationValue, hours) {
        val h = durationValue.inWholeHours.toInt()
        h.coerceIn(minHours, maxHours)
    }

    val resolvedMinutes = remember(durationValue, resolvedHours, minutes) {
        val remainder = (durationValue - resolvedHours.hours).inWholeMinutes.toInt()
        val range = validMinutesRangeForHour(resolvedHours)
        remainder.coerceIn(range.first, range.last)
    }

    var selectedHours by remember { mutableStateOf(resolvedHours) }
    var selectedMinutes by remember { mutableStateOf(resolvedMinutes) }

    LaunchedEffect(resolvedHours, resolvedMinutes) {
        selectedHours = resolvedHours
        selectedMinutes = resolvedMinutes
    }

    LaunchedEffect(selectedHours, selectedMinutes) {
        select(selectedHours.hours + selectedMinutes.minutes)
    }

    val hoursColumn = WheelColumn(
        id = "duration_hours",
        items = hours,
        selected = selectedHours,
        onSelect = { hour ->
            selectedHours = hour
            val validRange = validMinutesRangeForHour(hour)
            if (selectedMinutes !in validRange) {
                selectedMinutes = selectedMinutes.coerceIn(validRange.first, validRange.last)
            }
        },
        isValid = { hour ->
            !validMinutesRangeForHour(hour).isEmpty()
        },
        itemContent = { item, _ ->
            WheelItemRow(
                modifier = Modifier.fillMaxWidth()
                    .padding(end = AppTokens.dp.contentPadding.text),
                text = item.toString(),
                subText = hourLabel,
                isValid = true,
                horizontalArrangement = Arrangement.End
            )
        },
        listState = hoursState
    )

    val minutesColumn = WheelColumn(
        id = "duration_minutes",
        items = minutes,
        selected = selectedMinutes,
        onSelect = { minute ->
            if (minute in validMinutesRangeForHour(selectedHours)) {
                selectedMinutes = minute
            }
        },
        isValid = { minute ->
            minute in validMinutesRangeForHour(selectedHours)
        },
        itemContent = { item, _ ->
            WheelItemRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = AppTokens.dp.contentPadding.text),
                text = item.toString().padStart(2, '0'),
                subText = minuteLabel,
                isValid = true,
                horizontalArrangement = Arrangement.Start
            )
        },
        listState = minutesState
    )

    MultiWheelPicker(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTokens.dp.wheelPicker.height),
        selectorProperties = DefaultSelectorProperties(
            shape = RoundedCornerShape(AppTokens.dp.wheelPicker.radius),
            color = AppTokens.colors.background.card,
        ),
        columns = listOf(hoursColumn, minutesColumn)
    )
}

@AppPreview
@Composable
private fun DurationWheelPickerPreview() {
    PreviewContainer {
        DurationWheelPicker(
            hours = DefaultDurationHours,
            minutes = DefaultDurationMinutes,
            value = DurationFormatState.of(1.hours + 30.minutes),
            select = {},
        )
    }
}
