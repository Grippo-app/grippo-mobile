package com.grippo.trainings.trainings.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.stubMonthlyTrainingTimeline
import com.grippo.design.components.metrics.DigestMonthCard
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@Composable
internal fun MonthlyTrainingsPage(
    modifier: Modifier = Modifier,
    timeline: ImmutableList<TimelineState>,
    contentPadding: PaddingValues,
    month: LocalDate? = null,
    onDigestClick: () -> Unit,
    onOpenDaily: (LocalDate) -> Unit,
) {
    val scrollState = rememberScrollState()

    val monthlyContent = remember(timeline) {
        timeline.monthlyContent()
    }
    val digest = monthlyContent.digest
    val days = monthlyContent.days

    val monthReference = remember(month, digest, days) {
        month?.let { LocalDate(it.year, it.month, 1) }
            ?: digest?.month
            ?: days.firstOrNull()?.month
            ?: DateRange.Range.Monthly().range.from.date
    }

    LaunchedEffect(monthReference) {
        scrollState.scrollTo(0)
    }

    val calendarWeeks = remember(monthReference, days) {
        buildMonthCalendar(monthReference, days)
    }

    val weekDayLabels = remember {
        val mondayReference = LocalDate(2023, 1, 2)
        DayOfWeek.entries.map { day ->
            val labelDate = mondayReference.plus(DatePeriod(days = day.ordinal))
            DateTimeUtils.format(labelDate, DateFormat.DateOnly.WeekdayShort)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.screen.verticalPadding),
    ) {
        AnimatedContent(
            modifier = Modifier.fillMaxWidth(),
            targetState = digest,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
        ) { d ->
            if (d != null) {
                DigestMonthCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalableClick(onClick = onDigestClick),
                    value = d.summary,
                )
            }
        }

        MonthCalendar(
            modifier = Modifier
                .fillMaxWidth(),
            weekDayLabels = weekDayLabels,
            weeks = calendarWeeks,
            onDayClick = onOpenDaily,
        )
    }
}

@Composable
private fun MonthCalendar(
    modifier: Modifier = Modifier,
    weekDayLabels: List<String>,
    weeks: List<List<MonthlyCalendarDay>>,
    onDayClick: (LocalDate) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val horizontalSpacing = AppTokens.dp.contentPadding.subContent
        val dayCellWidth = remember(maxWidth, horizontalSpacing) {
            val spacing = horizontalSpacing * (weekDayLabels.size - 1)
            (maxWidth - spacing) / weekDayLabels.size
        }
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
    day: MonthlyCalendarDay,
    onDayClick: (LocalDate) -> Unit,
) {
    val date = day.date
    val trainingCount = day.trainingDay?.trainings?.size ?: 0
    val hasTrainings = trainingCount > 0
    val isCurrentDay = day.isCurrentMonth && DateTimeUtils.isToday(date)
    val shape = RoundedCornerShape(AppTokens.dp.contentPadding.subContent)
    val defaultBorder = AppTokens.colors.border.default.copy(alpha = 0.3f)
    val backgroundColor = when {
        isCurrentDay -> AppTokens.colors.brand.color6.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isCurrentDay -> AppTokens.colors.brand.color6
        hasTrainings -> AppTokens.colors.border.default
        else -> defaultBorder
    }
    val textColor = when {
        !day.isCurrentMonth -> AppTokens.colors.text.disabled
        isCurrentDay -> AppTokens.colors.text.primary
        hasTrainings -> AppTokens.colors.text.primary
        else -> AppTokens.colors.text.secondary
    }
    val clickableModifier = if (hasTrainings && day.isCurrentMonth) {
        Modifier.scalableClick { onDayClick(date) }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .then(clickableModifier)
            .clip(shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .background(color = backgroundColor, shape = shape)
            .padding(AppTokens.dp.contentPadding.subContent),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = date.day.toString(),
            style = AppTokens.typography.b14Med(),
            color = textColor,
        )

        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.text))

        if (hasTrainings) {
            CalendarTrainingBars(
                count = trainingCount,
                barColor = AppTokens.colors.brand.color6,
            )
        }
    }
}

@Immutable
private data class MonthlyCalendarContent(
    val digest: TimelineState.MonthlyDigest?,
    val days: ImmutableList<TimelineState.MonthlyTrainingsDay>,
)

private fun ImmutableList<TimelineState>.monthlyContent(): MonthlyCalendarContent {
    var digest: TimelineState.MonthlyDigest? = null
    val days = mutableListOf<TimelineState.MonthlyTrainingsDay>()

    for (value in this) {
        when (value) {
            is TimelineState.MonthlyDigest -> if (digest == null) {
                digest = value
            }

            is TimelineState.MonthlyTrainingsDay -> days += value
            else -> Unit
        }
    }

    return MonthlyCalendarContent(
        digest = digest,
        days = days.toPersistentList(),
    )
}

private fun buildMonthCalendar(
    monthReference: LocalDate,
    days: List<TimelineState.MonthlyTrainingsDay>,
): List<List<MonthlyCalendarDay>> {
    val trainingsByDate = days.associateBy { it.date }
    val startOfMonth = LocalDate(monthReference.year, monthReference.month, 1)
    val daysInMonth = DateTimeUtils.getDaysInMonth(monthReference.year, monthReference.month)
    val startOffset = (startOfMonth.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber)
        .let { if (it < 0) it + 7 else it }
    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
    val firstDisplayedDate = startOfMonth.minus(DatePeriod(days = startOffset))

    return (0 until totalCells).map { index ->
        val date = firstDisplayedDate.plus(DatePeriod(days = index))
        MonthlyCalendarDay(
            date = date,
            isCurrentMonth = date.month == monthReference.month && date.year == monthReference.year,
            trainingDay = trainingsByDate[date],
        )
    }.chunked(7)
}

@Immutable
private data class MonthlyCalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val trainingDay: TimelineState.MonthlyTrainingsDay?,
)

@AppPreview
@Composable
private fun MonthlyTrainingsPagePreview() {
    PreviewContainer {
        MonthlyTrainingsPage(
            timeline = stubMonthlyTrainingTimeline(),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            month = DateRange.Range.Monthly().range.from.date,
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
            timeline = persistentListOf(),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            month = DateRange.Range.Monthly().range.from.date,
            onDigestClick = {},
            onOpenDaily = { _ -> },
        )
    }
}
