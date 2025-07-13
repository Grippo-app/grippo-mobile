package com.grippo.date.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeDialog
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.date_picker_title
import com.grippo.design.resources.submit_btn
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

@Composable
internal fun DatePickerScreen(
    state: DatePickerState,
    loaders: ImmutableSet<DatePickerLoader>,
    contract: DatePickerContract
) = BaseComposeDialog(ScreenBackground.Color(AppTokens.colors.background.secondary)) {

    Toolbar(
        modifier = Modifier,
        title = AppTokens.strings.res(Res.string.date_picker_title),
        style = ToolbarStyle.Transparent,
    )

    val initialMillis = remember(state.initial) {
        state.initial.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    val minMillis = remember(state.limitations) {
        state.limitations.first.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    val maxMillis = remember(state.limitations) {
        state.limitations.second.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = state.limitations.first.year..state.limitations.second.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis in minMillis..maxMillis
            }
        }
    )

    val calendar = AppTokens.colors.calendar

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.contentPadding.content
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DatePicker(
            modifier = Modifier.fillMaxWidth(),
            state = datePickerState,
            showModeToggle = false,
            title = null,
            headline = null,
            colors = DatePickerDefaults.colors(
                containerColor = calendar.container,
                selectedDayContainerColor = calendar.selectedDayBackground,
                selectedDayContentColor = calendar.selectedDayText,
                todayDateBorderColor = calendar.todayBorder,
                todayContentColor = calendar.todayText,
                disabledDayContentColor = calendar.disabledDayText,
                dayContentColor = calendar.dayText,
                weekdayContentColor = calendar.weekdayText
            )
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.submit_btn),
            style = ButtonStyle.Primary,
            onClick = contract::submit
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        DatePickerScreen(
            state = DatePickerState(
                initial = LocalDateTime(2025, 7, 9, 14, 30),
            ),
            loaders = persistentSetOf(),
            contract = DatePickerContract.Empty
        )
    }
}