package com.grippo.trainings.trainings.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.digest.MonthDigestCard
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.domain.state.training.transformation.transformToTrainingListValue
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

private val DayCellHeight = 96.dp

@Composable
internal fun MonthlyTrainingsPage(
    modifier: Modifier = Modifier,
    trainings: ImmutableList<TrainingListValue>,
    contentPadding: PaddingValues,
    onDigestClick: () -> Unit,
    onOpenDaily: (LocalDate) -> Unit,
) {
    val gridState = rememberLazyListState()

    val digest = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.MonthlyDigest>().firstOrNull()
    }
    val days = remember(trainings) {
        trainings.filterIsInstance<TrainingListValue.MonthlyTrainingsDay>()
    }

    val monthReference = remember(digest, days) {
        digest?.month ?: days.firstOrNull()?.month ?: DateTimeUtils.thisMonth().from.date
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = gridState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.screen.verticalPadding),
    ) {
        digest?.let { value ->
            item(key = "digest") {
                MonthDigestCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalableClick(onClick = onDigestClick),
                    value = value.summary,
                )
            }
        }

        item(key = "monthly_calendar_${monthReference.year}_${monthReference.month.number}") {
            MonthCalendar(
                modifier = Modifier
                    .fillMaxWidth(),
                weekDayLabels = weekDayLabels,
                weeks = calendarWeeks,
                onDayClick = onOpenDaily,
            )
        }
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
        val dayCellHeight = DayCellHeight
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
    val isToday = DateTimeUtils.isToday(date)
    val shape = RoundedCornerShape(AppTokens.dp.contentPadding.subContent)
    val defaultBorder = AppTokens.colors.border.default.copy(alpha = 0.3f)
    val backgroundColor = when {
        isToday -> AppTokens.colors.brand.color6.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isToday -> AppTokens.colors.brand.color6
        hasTrainings -> AppTokens.colors.border.default
        else -> defaultBorder
    }
    val textColor = when {
        !day.isCurrentMonth -> AppTokens.colors.text.disabled
        isToday -> AppTokens.colors.text.primary
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

private fun buildMonthCalendar(
    monthReference: LocalDate,
    days: List<TrainingListValue.MonthlyTrainingsDay>,
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
    val trainingDay: TrainingListValue.MonthlyTrainingsDay?,
)

@AppPreview
@Composable
private fun MonthlyTrainingsPagePreview() {
    PreviewContainer {
        MonthlyTrainingsPage(
            trainings = listOf(
                stubTraining(),
                stubTraining(),
            ).transformToTrainingListValue(
                range = DateTimeUtils.thisMonth()
            ),
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
            trainings = persistentListOf(),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            onDigestClick = {},
            onOpenDaily = { _ -> },
        )
    }
}
