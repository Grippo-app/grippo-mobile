package com.grippo.design.components.metrics.streak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.TrainingStreakProgressState
import com.grippo.core.state.metrics.stubTrainingStreaks
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_streak_period_value
import com.grippo.design.resources.provider.training_streak_timeline_title
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat

@Composable
public fun TrainingStreakTimelineCard(
    entries: List<TrainingStreakProgressState>,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.training_streak_timeline_title),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary
        )

        entries.asReversed().forEach { entry ->
            TrainingStreakTimelineRow(
                entry = entry
            )
        }
    }
}

@Composable
private fun TrainingStreakTimelineRow(
    entry: TrainingStreakProgressState,
) {
    val from = entry.range.from.date
    val to = entry.range.to.date

    val formattedDate = if (from == to) {
        val start = DateCompose.rememberFormat(from, DateFormat.DateOnly.DateDdMmm)
        start
    } else {
        val start = DateCompose.rememberFormat(from, DateFormat.DateOnly.DateDdMmm)
        val end = DateCompose.rememberFormat(to, DateFormat.DateOnly.DateDdMmm)
        "$start - $end"
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
        Text(
            text = formattedDate,
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.primary
        )

        Text(
            text = AppTokens.strings.res(
                Res.string.training_streak_period_value,
                entry.achievedSessions,
                entry.targetSessions
            ),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary
        )

        val progressColors = when {
            entry.progressPercent >= 80 -> AppTokens.colors.lineIndicator.success
            entry.progressPercent >= 40 -> AppTokens.colors.lineIndicator.info
            else -> AppTokens.colors.lineIndicator.warning
        }

        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = (entry.progressPercent.coerceIn(0, 100)) / 100f,
            colors = progressColors
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakTimelineCardPreview() {
    PreviewContainer {
        TrainingStreakTimelineCard(
            entries = stubTrainingStreaks().random().timeline
        )
    }
}
