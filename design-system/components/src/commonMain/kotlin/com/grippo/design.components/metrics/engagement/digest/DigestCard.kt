package com.grippo.design.components.metrics.engagement.digest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.core.state.metrics.engagement.stubDigest
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.metrics.internal.MetricBreakdownItem
import com.grippo.design.components.metrics.internal.MetricBreakdownRow
import com.grippo.design.components.metrics.internal.MetricOverviewHeader
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.skeleton.SkeletonPanel
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.digest_empty
import com.grippo.design.resources.provider.digest_title
import com.grippo.design.resources.provider.digest_unavailable
import com.grippo.design.resources.provider.digest_zero_duration
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.tonnage
import com.grippo.design.resources.provider.trainings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
public fun DigestCard(
    modifier: Modifier = Modifier,
    value: DigestState,
    onClick: () -> Unit,
) {
    MetricSectionPanel(
        modifier = modifier.scalableClick(onClick = onClick),
        style = MetricSectionPanelStyle.Large,
    ) {
        MetricOverviewHeader(title = AppTokens.strings.res(Res.string.digest_title))

        when (value) {
            is DigestState.Empty -> EmptyDigestBody()
            is DigestState.Content -> ContentDigestBody(value = value)
        }
    }
}

@Composable
public fun DigestCardSkeleton(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val header = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val count = rememberTextHeight(style = AppTokens.typography.h3(), textMeasurer = textMeasurer)
    val label = rememberTextHeight(style = AppTokens.typography.b13Med(), textMeasurer = textMeasurer)
    val breakdownLabel = rememberTextHeight(style = AppTokens.typography.b11Med(), textMeasurer = textMeasurer)
    val contentHeight = maxOf(header, AppTokens.dp.metrics.overview.detailsIcon) +
        count + label + header + breakdownLabel + AppTokens.dp.contentPadding.text * 2 +
        AppTokens.dp.divider.thickness + AppTokens.dp.metrics.panel.large.spacer * 3

    SkeletonPanel(modifier = modifier, contentHeight = contentHeight)
}

@Composable
private fun EmptyDigestBody() {
    Text(
        text = AppTokens.strings.res(Res.string.digest_empty),
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
    )
}

@Composable
private fun ColumnScope.ContentDigestBody(
    value: DigestState.Content,
) {
    val durationValue = value.duration.value
    val duration = when (durationValue) {
        null -> AppTokens.strings.res(Res.string.digest_unavailable)
        Duration.ZERO -> AppTokens.strings.res(Res.string.digest_zero_duration)
        else -> value.duration.short()
    }
    val volumeValue = value.total.value

    DigestBody(
        trainingsCount = value.trainingsCount,
        breakdown = persistentListOf(
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.duration),
                value = duration,
                dimmed = durationValue == null || durationValue == Duration.ZERO,
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.tonnage),
                value = VolumeFormatState.shortTotal(volumeValue),
                dimmed = volumeValue == null || volumeValue == 0f,
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.sets),
                value = value.totalSets.toString(),
                dimmed = value.totalSets == 0,
            ),
        ),
    )
}

@Composable
private fun ColumnScope.DigestBody(
    trainingsCount: Int,
    breakdown: ImmutableList<MetricBreakdownItem>,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = trainingsCount.toString(),
            style = AppTokens.typography.h3(),
            color = if (trainingsCount == 0) {
                AppTokens.colors.text.tertiary
            } else {
                AppTokens.colors.text.primary
            },
        )

        Text(
            text = AppTokens.strings.res(Res.string.trainings),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )
    }

    ContentDivider()

    MetricBreakdownRow(
        modifier = Modifier.fillMaxWidth(),
        items = breakdown,
    )
}

@AppPreview
@Composable
private fun DigestCardPreview() {
    PreviewContainer {
        DigestCard(
            value = stubDigest(),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun DigestCardSkeletonPreview() {
    PreviewContainer {
        DigestCardSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

@AppPreview
@Composable
private fun DigestCardEmptyPreview() {
    PreviewContainer {
        DigestCard(
            value = DigestState.Empty,
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun DigestCardMissingDurationPreview() {
    PreviewContainer {
        DigestCard(
            value = stubDigest().copy(duration = DurationFormatState.Empty()),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun DigestCardZeroLoadPreview() {
    PreviewContainer {
        DigestCard(
            value = stubDigest().copy(
                duration = DurationFormatState.ofTotal(Duration.ZERO),
                total = VolumeFormatState.ofTotal(0f),
                totalSets = 0,
            ),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun DigestCardLongValuesPreview() {
    PreviewContainer {
        DigestCard(
            value = stubDigest().copy(
                duration = DurationFormatState.ofTotal(123.hours + 59.minutes),
                total = VolumeFormatState.ofTotal(184_829.5f),
                totalSets = 12_345,
                trainingsCount = 123,
            ),
            onClick = {},
        )
    }
}
