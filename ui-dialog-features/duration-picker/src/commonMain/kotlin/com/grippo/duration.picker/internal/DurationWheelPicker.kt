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
import com.grippo.design.components.wheel.WheelItemRow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.hours_short
import com.grippo.design.resources.provider.minutes_short
import com.grippo.duration.picker.DefaultDurationMinutes
import com.grippo.duration.picker.resolveDurationHours
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
    value: Duration,
    select: (Duration) -> Unit
) {
    val hoursState = rememberLazyListState()
    val minutesState = rememberLazyListState()

    val hourLabel = AppTokens.strings.res(Res.string.hours_short)
    val minuteLabel = AppTokens.strings.res(Res.string.minutes_short)

    val resolvedHours = remember(value, hours) {
        val min = hours.firstOrNull() ?: 0
        val max = hours.lastOrNull() ?: 0
        value.inWholeHours.toInt().coerceIn(min, max)
    }

    val resolvedMinutes = remember(value, resolvedHours, minutes) {
        val min = minutes.firstOrNull() ?: 0
        val max = minutes.lastOrNull() ?: 0
        val remainder = (value - resolvedHours.hours).inWholeMinutes.toInt()
        remainder.coerceIn(min, max)
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
        onSelect = { selectedHours = it },
        isValid = { true },
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
        onSelect = { selectedMinutes = it },
        isValid = { true },
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
            hours = resolveDurationHours(1.hours + 30.minutes),
            minutes = DefaultDurationMinutes,
            value = 1.hours + 30.minutes,
            select = {},
        )
    }
}
