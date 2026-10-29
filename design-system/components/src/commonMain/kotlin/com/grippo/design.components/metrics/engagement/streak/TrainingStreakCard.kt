package com.grippo.design.components.metrics.engagement.streak

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.grippo.core.state.metrics.engagement.TrainingStreakFeaturedState
import com.grippo.core.state.metrics.engagement.TrainingStreakMood
import com.grippo.core.state.metrics.engagement.TrainingStreakState
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.internal.MetricOverviewHeader
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonPanel
import com.grippo.design.components.skeleton.SkeletonPanelStyle
import com.grippo.design.components.utils.ValueSlot
import com.grippo.design.components.utils.buildValueSlotString
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Flame
import com.grippo.design.resources.provider.joined_pair
import com.grippo.design.resources.provider.metric_overview_streak_active_days
import com.grippo.design.resources.provider.metric_overview_streak_title
import com.grippo.design.resources.provider.training_streak_growth_extended
import com.grippo.design.resources.provider.training_streak_growth_kept
import com.grippo.design.resources.provider.training_streak_growth_reset
import com.grippo.design.resources.provider.training_streak_growth_started
import com.grippo.design.resources.provider.value_transition
import kotlinx.collections.immutable.toPersistentList

@Immutable
public sealed interface TrainingStreakCardStyle {
    @Immutable
    public data object Overview : TrainingStreakCardStyle

    @Immutable
    public data object Detailed : TrainingStreakCardStyle

    @Immutable
    public data class Growth(
        public val previous: TrainingStreakState?,
    ) : TrainingStreakCardStyle
}

private const val STREAK_GROWTH_DURATION_MS = 900
private const val STREAK_GROWTH_LABEL = "TrainingStreakGrowthLength"

@Composable
public fun TrainingStreakCard(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
    style: TrainingStreakCardStyle = TrainingStreakCardStyle.Detailed,
) {
    when (style) {
        TrainingStreakCardStyle.Overview -> TrainingStreakOverview(modifier = modifier, value = value)
        TrainingStreakCardStyle.Detailed -> TrainingStreakDetailSummary(modifier = modifier, value = value)
        is TrainingStreakCardStyle.Growth -> TrainingStreakGrowth(
            modifier = modifier,
            value = value,
            previous = style.previous,
        )
    }
}

@Composable
private fun TrainingStreakOverview(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
) {
    val title = AppTokens.strings.res(Res.string.metric_overview_streak_title)

    val headline = value.featured.headline()
    val cadenceLabel = value.featured.cadence()

    val progressValue =
        (value.featured.progressPercent.coerceIn(0, 100)) / 100f

    val progressColors = value.featured.mood.indicatorColors()

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
    ) {
        MetricOverviewHeader(title = title)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(2.5f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(AppTokens.dp.metrics.overview.rhythmIcon),
                        imageVector = AppTokens.icons.Flame,
                        tint = AppTokens.colors.icon.secondary,
                        contentDescription = null,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = headline,
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                    )
                }
                Text(
                    text = cadenceLabel,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = value.totalActiveDays.toString(),
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = AppTokens.strings.res(Res.string.metric_overview_streak_active_days),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                    textAlign = TextAlign.End,
                )
            }
        }

        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = progressValue,
            colors = progressColors,
        )

        value.timeline.lastOrNull()
            ?.let { currentPeriod -> value.featured.currentPeriodLine(currentPeriod) }
            ?.let { line ->
                Text(
                    text = line,
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
    }
}

@Composable
private fun TrainingStreakDetailSummary(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
) {
    MetricSectionPanel(modifier = modifier, style = MetricSectionPanelStyle.Large) {
        Text(
            text = value.summaryHeadline(),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
        )
        Text(
            text = value.summaryDescription(),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
        )
        if (value.featured.length > 0) {
            Text(
                text = value.activeDaysDescription(),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@Composable
private fun TrainingStreakGrowth(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
    previous: TrainingStreakState?,
) {
    val length = value.featured.length
    val previousLength = previous?.featured?.length

    var revealed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val currentLengthAlpha = animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = STREAK_GROWTH_DURATION_MS, easing = FastOutSlowInEasing),
        label = STREAK_GROWTH_LABEL,
    )

    val lengthColor = when {
        previousLength == null || length == previousLength -> AppTokens.colors.text.primary
        length > previousLength -> AppTokens.colors.semantic.success
        else -> AppTokens.colors.semantic.decline
    }
    val nextTrainingLine = value.nextTrainingLine()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.metrics.section.spacer),
    ) {
        MetricOverviewHeader(title = AppTokens.strings.res(Res.string.metric_overview_streak_title))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.metrics.overview.rhythmIcon),
                imageVector = AppTokens.icons.Flame,
                tint = value.featured.mood.indicatorColors().dominant,
                contentDescription = null,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = value.featured.headline(),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
                Text(
                    text = value.featured.cadence(),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }

        val outcomeFrom = previousLength?.takeIf { earlier -> earlier > 0 || length > 0 }

        if (outcomeFrom != null || nextTrainingLine != null) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
                if (outcomeFrom != null) {
                    StreakOutcomeLine(
                        previousLength = outcomeFrom,
                        length = length,
                        currentColor = lengthColor,
                        currentLengthAlpha = currentLengthAlpha,
                    )
                }

                if (nextTrainingLine != null) {
                    Text(
                        text = nextTrainingLine,
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakOutcomeLine(
    modifier: Modifier = Modifier,
    previousLength: Int,
    length: Int,
    currentColor: Color,
    currentLengthAlpha: State<Float>,
) {
    val lineColor = AppTokens.colors.text.secondary
    val text = when {
        previousLength == 0 -> AnnotatedString(AppTokens.strings.res(Res.string.training_streak_growth_started))
        length == previousLength -> AnnotatedString(AppTokens.strings.res(Res.string.training_streak_growth_kept))
        else -> {
            val outcomeResource = if (length < previousLength) {
                Res.string.training_streak_growth_reset
            } else {
                Res.string.training_streak_growth_extended
            }
            val outcome = AppTokens.strings.res(outcomeResource)
            val change = AppTokens.strings.res(
                Res.string.value_transition,
                previousLength.toString(),
                ValueSlot.marker(0),
            )
            val line = AppTokens.strings.res(Res.string.joined_pair, outcome, change)
            val revealedColor = currentColor.copy(alpha = currentColor.alpha * currentLengthAlpha.value)

            buildValueSlotString(
                sentence = line,
                slots = listOf(
                    ValueSlot(value = length.toString()) { current ->
                        withStyle(SpanStyle(color = revealedColor)) { append(current) }
                    },
                ),
            )
        }
    }

    Text(
        modifier = modifier,
        text = text,
        style = AppTokens.typography.b12Med(),
        color = lineColor,
    )
}

@Composable
public fun TrainingStreakCardSkeleton(
    modifier: Modifier = Modifier,
    style: TrainingStreakCardStyle,
) {
    val textMeasurer = rememberTextMeasurer()
    val titleLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val headlineLine = rememberTextHeight(style = AppTokens.typography.h4(), textMeasurer = textMeasurer)
    val cadenceLine = rememberTextHeight(style = AppTokens.typography.b13Med(), textMeasurer = textMeasurer)
    val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)

    val spacer = AppTokens.dp.metrics.panel.large.spacer
    val textGap = AppTokens.dp.contentPadding.text
    val headerHeight = maxOf(titleLine, AppTokens.dp.metrics.overview.detailsIcon)
    val indicatorHeight = AppTokens.dp.metrics.charts.indicator.height

    when (style) {
        TrainingStreakCardStyle.Overview -> {
            val countLine = rememberTextHeight(style = AppTokens.typography.h3(), textMeasurer = textMeasurer)
            val headlineHeight = maxOf(AppTokens.dp.metrics.overview.rhythmIcon, headlineLine) + textGap + cadenceLine
            val activeDaysHeight = countLine + textGap + captionLine

            SkeletonPanel(
                modifier = modifier,
                contentHeight = headerHeight + maxOf(headlineHeight, activeDaysHeight) + indicatorHeight +
                    captionLine + spacer * 3,
                style = SkeletonPanelStyle.Large,
            )
        }

        TrainingStreakCardStyle.Detailed -> {
            val summaryLine = rememberTextHeight(style = AppTokens.typography.h3(), textMeasurer = textMeasurer)
            val descriptionLine = rememberTextHeight(style = AppTokens.typography.b14Med(), textMeasurer = textMeasurer)

            SkeletonPanel(
                modifier = modifier,
                contentHeight = summaryLine + descriptionLine + cadenceLine + spacer * 2,
                style = SkeletonPanelStyle.Large,
            )
        }

        is TrainingStreakCardStyle.Growth -> {
            val headlineHeight = maxOf(AppTokens.dp.metrics.overview.rhythmIcon, headlineLine + textGap + cadenceLine)
            val sectionSpacer = AppTokens.dp.metrics.section.spacer

            SkeletonBone(
                modifier = modifier
                    .fillMaxWidth()
                    .height(headerHeight + headlineHeight + captionLine + textGap + captionLine + sectionSpacer * 2),
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardSkeletonOverviewPreview() {
    PreviewContainer {
        TrainingStreakCardSkeleton(
            modifier = Modifier.fillMaxWidth(),
            style = TrainingStreakCardStyle.Overview,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardSkeletonDetailedPreview() {
    PreviewContainer {
        TrainingStreakCardSkeleton(
            modifier = Modifier.fillMaxWidth(),
            style = TrainingStreakCardStyle.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardSkeletonGrowthPreview() {
    PreviewContainer {
        TrainingStreakCardSkeleton(
            modifier = Modifier.fillMaxWidth(),
            style = TrainingStreakCardStyle.Growth(previous = null),
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardDetailedDailyPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[0],
            style = TrainingStreakCardStyle.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardDetailedWeeklyPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[1],
            style = TrainingStreakCardStyle.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardDetailedRhythmPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[2],
            style = TrainingStreakCardStyle.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardDetailedPatternPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[3],
            style = TrainingStreakCardStyle.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardDetailedRestartPreview() {
    val streak = stubTrainingStreaks()[1]
    val featured = streak.featured as TrainingStreakFeaturedState.Weekly
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = streak.copy(featured = featured.copy(length = 0)),
            style = TrainingStreakCardStyle.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardOverviewDailyPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[0],
            style = TrainingStreakCardStyle.Overview,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardOverviewWeeklyPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[1],
            style = TrainingStreakCardStyle.Overview,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardOverviewRhythmPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[2],
            style = TrainingStreakCardStyle.Overview,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardOverviewPatternPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[3],
            style = TrainingStreakCardStyle.Overview,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardOverviewRestartPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[4],
            style = TrainingStreakCardStyle.Overview,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardGrowthExtendedPreview() {
    val streak = stubTrainingStreaks()[0]
    val featured = streak.featured as TrainingStreakFeaturedState.Daily
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = streak,
            style = TrainingStreakCardStyle.Growth(
                previous = streak.copy(
                    totalActiveDays = streak.totalActiveDays - 1,
                    featured = featured.copy(length = featured.length - 1),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardGrowthKeptPreview() {
    val streak = stubTrainingStreaks()[2]
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = streak,
            style = TrainingStreakCardStyle.Growth(previous = streak),
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardGrowthResetPreview() {
    val streak = stubTrainingStreaks()[1]
    val featured = streak.featured as TrainingStreakFeaturedState.Weekly
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = streak.copy(
                featured = featured.copy(length = 1, mood = TrainingStreakMood.Restart),
            ),
            style = TrainingStreakCardStyle.Growth(previous = streak.copy(featured = featured.copy(length = 6))),
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardGrowthStartedPreview() {
    val streak = stubTrainingStreaks()[1]
    val featured = streak.featured as TrainingStreakFeaturedState.Weekly
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = streak.copy(featured = featured.copy(length = 1)),
            style = TrainingStreakCardStyle.Growth(previous = streak.copy(featured = featured.copy(length = 0))),
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardGrowthNoStreakPreview() {
    val streak = stubTrainingStreaks()[1]
    val featured = streak.featured as TrainingStreakFeaturedState.Weekly
    val noStreak = streak.copy(featured = featured.copy(length = 0))
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = noStreak,
            style = TrainingStreakCardStyle.Growth(previous = noStreak),
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardGrowthWeekDonePreview() {
    val streak = stubTrainingStreaks()[1]
    val currentWeek = streak.timeline.last()
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = streak.copy(
                timeline = (streak.timeline.dropLast(1) + currentWeek.copy(achievedTrainingDays = 4))
                    .toPersistentList(),
            ),
            style = TrainingStreakCardStyle.Growth(previous = streak),
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardGrowthWithoutPreviousPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[3],
            style = TrainingStreakCardStyle.Growth(previous = null),
        )
    }
}
