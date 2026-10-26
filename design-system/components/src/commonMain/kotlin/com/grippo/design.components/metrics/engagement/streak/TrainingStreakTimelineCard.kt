package com.grippo.design.components.metrics.engagement.streak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.engagement.TrainingStreakFeaturedState
import com.grippo.core.state.metrics.engagement.TrainingStreakProgressState
import com.grippo.core.state.metrics.engagement.TrainingStreakState
import com.grippo.core.state.metrics.engagement.stubTrainingStreakProgress
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_streak_details_day_description
import com.grippo.design.resources.provider.training_streak_details_day_recorded
import com.grippo.design.resources.provider.training_streak_details_day_unrecorded
import com.grippo.design.resources.provider.training_streak_partial_period
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.atTime

@Composable
public fun TrainingStreakTimelineCard(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
) {
    if (value.timeline.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Text(
            text = value.featured.timelineTitle(),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
        )
        if (value.featured is TrainingStreakFeaturedState.Daily) {
            TrainingStreakDayStrip(value = value)
        } else {
            Text(
                text = value.featured.timelineDescription(),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
            value.timeline.takeLast(4).asReversed().forEach { entry ->
                TrainingStreakTimelineRow(value = value, entry = entry)
            }
        }
    }
}

@Composable
private fun TrainingStreakDayStrip(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
) {
    val entries = value.timeline.takeLast(7)
    val dateScope = DateRangeFormatState.of(
        from = entries.first().range.value?.from,
        to = entries.last().range.value?.to,
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Text(
            text = dateScope.short(),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            entries.forEach { entry ->
                TrainingStreakDay(modifier = Modifier.weight(1f), entry = entry)
            }
        }
        Text(
            text = value.featured.timelineDescription(),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun TrainingStreakDay(
    modifier: Modifier = Modifier,
    entry: TrainingStreakProgressState,
) {
    val date = entry.range.value?.from?.date
    val fullDate = if (date != null) {
        DateCompose.rememberFormat(
            value = date,
            format = DateFormat.DateOnly.DateDdMmmYyyy,
            contextual = false,
        )
    } else {
        entry.range.short()
    }
    val isRecorded = entry.achievedTrainingDays > 0
    val recordLabel = AppTokens.strings.res(
        if (isRecorded) {
            Res.string.training_streak_details_day_recorded
        } else {
            Res.string.training_streak_details_day_unrecorded
        },
    )
    val description = AppTokens.strings.res(
        Res.string.training_streak_details_day_description,
        fullDate,
        recordLabel,
    )

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = if (isRecorded) 1f else 0f,
            colors = AppTokens.colors.charts.indicator.info,
        )
        Text(
            text = date?.day?.toString() ?: entry.range.short(),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@Composable
private fun TrainingStreakTimelineRow(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
    entry: TrainingStreakProgressState,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = DateRangeFormatState.of(
                from = entry.range.value?.from,
                to = entry.range.value?.to,
            ).short(),
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.primary,
        )
        Text(
            text = value.featured.timelineValue(entry),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = entry.progressPercent / 100f,
            colors = AppTokens.colors.charts.indicator.info,
        )
        if (entry.isPartial) {
            Text(
                text = AppTokens.strings.res(Res.string.training_streak_partial_period),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingStreakTimelineCardDailyPreview() {
    val value = stubTrainingStreaks()[0]
    val firstDate = LocalDate(2026, 9, 28)
    PreviewContainer {
        TrainingStreakTimelineCard(
            value = value.copy(
                timeline = (0..6).map { index ->
                    val date = firstDate.plus(DatePeriod(days = index))
                    stubTrainingStreakProgress().copy(
                        achievedTrainingDays = if (index in listOf(0, 2, 3, 5)) 1 else 0,
                        targetTrainingDays = 1,
                        isPartial = index == 6,
                        range = DateRangeFormatState.of(
                            from = date.atTime(0, 0),
                            to = date.atTime(23, 59),
                        ),
                    )
                }.toPersistentList(),
            ),
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakTimelineCardWeeklyPreview() {
    PreviewContainer {
        TrainingStreakTimelineCard(value = stubTrainingStreaks()[1])
    }
}

@AppPreview
@Composable
private fun TrainingStreakTimelineCardCyclePreview() {
    PreviewContainer {
        TrainingStreakTimelineCard(value = stubTrainingStreaks()[2])
    }
}

@AppPreview
@Composable
private fun TrainingStreakTimelineCardClippedStartPreview() {
    val value = stubTrainingStreaks()[1]
    PreviewContainer {
        TrainingStreakTimelineCard(
            value = value.copy(
                timeline = persistentListOf(
                    value.timeline.first().copy(achievedTrainingDays = 2, isPartial = true),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakTimelineCardEmptyPreview() {
    PreviewContainer {
        TrainingStreakTimelineCard(value = stubTrainingStreaks()[0].copy(timeline = persistentListOf()))
    }
}
