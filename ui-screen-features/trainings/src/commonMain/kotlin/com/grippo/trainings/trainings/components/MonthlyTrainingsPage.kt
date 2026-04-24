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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.state.trainings.stubMonthlyTrainingTimeline
import com.grippo.design.components.metrics.engagement.digest.DigestCard
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.trainings.trainings.MonthlyCalendarDayState
import com.grippo.trainings.trainings.TrainingsTimelinePeriod
import com.grippo.trainings.trainings.buildMonthlyPeriod
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate

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
        AnimatedContent(
            modifier = Modifier.fillMaxWidth(),
            targetState = period.digest,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
        ) { digest ->
            if (digest != null) {
                DigestCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalableClick(onClick = onDigestClick),
                    value = digest,
                )
            }
        }

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
        val dayCellWidth = run {
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
    day: MonthlyCalendarDayState,
    onDayClick: (LocalDate) -> Unit,
) {
    val hasTrainings = day.trainingCount > 0
    val isHighlighted = day.isCurrentMonth && day.isToday
    val shape = RoundedCornerShape(AppTokens.dp.contentPadding.subContent)
    val defaultBorder = AppTokens.colors.border.default.copy(alpha = 0.3f)
    val backgroundColor = when {
        isHighlighted -> AppTokens.colors.brand.color6.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isHighlighted -> AppTokens.colors.brand.color6
        hasTrainings -> AppTokens.colors.border.default
        else -> defaultBorder
    }
    val textColor = when {
        !day.isCurrentMonth -> AppTokens.colors.text.disabled
        isHighlighted -> AppTokens.colors.text.primary
        hasTrainings -> AppTokens.colors.text.primary
        else -> AppTokens.colors.text.secondary
    }
    val clickableModifier = if (day.isClickable) {
        Modifier.scalableClick { day.date.value?.let(onDayClick) }
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
            period = buildMonthlyPeriod(
                range = DateRangePresets.monthly(),
                timeline = stubMonthlyTrainingTimeline(),
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
            period = TrainingsTimelinePeriod.Monthly(),
            contentPadding = PaddingValues(AppTokens.dp.contentPadding.content),
            onDigestClick = {},
            onOpenDaily = { _ -> },
        )
    }
}
