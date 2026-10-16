package com.grippo.trainings.trainings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.trainings.stubMonthlyTrainingTimeline
import com.grippo.design.components.frames.FocusFrame
import com.grippo.design.components.metrics.engagement.digest.DigestCard
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.trainings.trainings.MonthlyCalendarDayState
import com.grippo.trainings.trainings.TrainingsTimelinePeriod
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@Composable
internal fun MonthlyTrainingsPage(
    modifier: Modifier = Modifier,
    period: TrainingsTimelinePeriod.Monthly,
    contentPadding: PaddingValues,
    onDigestClick: () -> Unit,
    onOpenDaily: (LocalDate) -> Unit,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(period.monthReference) {
        scrollState.scrollTo(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.screen.verticalPadding),
    ) {
        DigestCard(
            modifier = Modifier
                .fillMaxWidth()
                .scalableClick(role = Role.Button, onClick = onDigestClick),
            value = period.digest,
        )

        MonthCalendar(
            modifier = Modifier.fillMaxWidth(),
            weekDayLabels = period.weekDayLabels,
            weeks = period.weeks,
            onDayClick = onOpenDaily,
        )
    }
}

@Composable
private fun MonthCalendar(
    modifier: Modifier = Modifier,
    weekDayLabels: ImmutableList<String>,
    weeks: ImmutableList<ImmutableList<MonthlyCalendarDayState>>,
    onDayClick: (LocalDate) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val horizontalSpacing = AppTokens.dp.contentPadding.subContent
        val totalHorizontalSpacing = horizontalSpacing * (weekDayLabels.size - 1)
        val dayCellWidth = (maxWidth - totalHorizontalSpacing) / weekDayLabels.size
        val dayCellHeight = AppTokens.dp.calendar.monthly.cellHeight
        val weekRowCount = weeks.size.takeIf { it > 0 } ?: 1
        val gridSpacing = AppTokens.dp.contentPadding.subContent * (weekRowCount - 1)
        val minCalendarHeight = (dayCellHeight * weekRowCount) +
                gridSpacing +
                AppTokens.dp.contentPadding.block * 2

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minCalendarHeight),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                weekDayLabels.forEach { label ->
                    Text(
                        modifier = Modifier.width(dayCellWidth),
                        text = label,
                        style = AppTokens.typography.b12Semi(),
                        color = AppTokens.colors.text.secondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.subContent))

            Column(
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
            ) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
                    ) {
                        week.forEach { day ->
                            key(day.date) {
                                MonthCalendarDayCell(
                                    modifier = Modifier
                                        .width(dayCellWidth)
                                        .height(dayCellHeight),
                                    day = day,
                                    onDayClick = onDayClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarDayCell(
    modifier: Modifier = Modifier,
    day: MonthlyCalendarDayState,
    onDayClick: (LocalDate) -> Unit,
) {
    val hasTrainings = day.trainingCount > 0
    val isHighlighted = day.isCurrentMonth && day.isToday

    val textColor = when {
        !day.isCurrentMonth -> AppTokens.colors.text.disabled
        isHighlighted || hasTrainings -> AppTokens.colors.text.primary
        else -> AppTokens.colors.text.secondary
    }
    val clickableModifier = if (day.isClickable) {
        Modifier.scalableClick(role = Role.Button) {
            day.date.value?.let(onDayClick)
        }
    } else {
        Modifier
    }

    val cellModifier = modifier.then(clickableModifier)

    FocusFrame(
        modifier = cellModifier,
        accent = when {
            isHighlighted -> AppTokens.colors.selection.accent
            hasTrainings -> AppTokens.colors.border.default
            else -> AppTokens.colors.background.card
        },
        background = if (isHighlighted) {
            AppTokens.colors.selection.background
        } else {
            AppTokens.colors.background.card
        },
    ) {
        MonthCalendarDayCellContent(
            day = day,
            textColor = textColor,
            hasTrainings = hasTrainings,
        )
    }
}

@Composable
private fun MonthCalendarDayCellContent(
    day: MonthlyCalendarDayState,
    textColor: Color,
    hasTrainings: Boolean,
) {
    Column(verticalArrangement = Arrangement.Top) {
        Text(
            text = day.dayOfMonth.toString(),
            style = AppTokens.typography.b14Med(),
            color = textColor,
        )

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.text))

        if (hasTrainings) {
            CalendarTrainingBars(
                count = day.trainingCount,
                barColor = AppTokens.colors.brand.color6,
            )
        }
    }
}

@AppPreview
@Composable
private fun MonthlyTrainingsPagePreview() {
    PreviewContainer {
        MonthlyTrainingsPage(
            period = monthlyTrainingsPreviewPeriod(),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            onDigestClick = {},
            onOpenDaily = { _ -> },
        )
    }
}

@AppPreview
@Composable
private fun MonthlyTrainingsEmptyPagePreview() {
    PreviewContainer {
        MonthlyTrainingsPage(
            period = monthlyTrainingsPreviewPeriod(empty = true),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            onDigestClick = {},
            onOpenDaily = { _ -> },
        )
    }
}

private fun monthlyTrainingsPreviewPeriod(empty: Boolean = false): TrainingsTimelinePeriod.Monthly {
    val date = LocalDateTime(2026, 10, 16, 12, 0)
    val range = TrainingsTimelinePeriod.Monthly().rangeFor(date)
    val timeline = if (empty) {
        persistentListOf()
    } else {
        stubMonthlyTrainingTimeline()
    }
    val period = TrainingsTimelinePeriod.Monthly.from(range = range, timeline = timeline)

    return period.copy(
        weeks = period.weeks.map { week ->
            week.map { day -> day.copy(isToday = day.date.value == date.date) }.toPersistentList()
        }.toPersistentList(),
    )
}
