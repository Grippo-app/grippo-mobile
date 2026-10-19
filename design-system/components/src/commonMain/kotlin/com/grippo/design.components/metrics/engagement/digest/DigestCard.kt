package com.grippo.design.components.metrics.engagement.digest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
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

/** Period summary that opens the statistics for the same range; the header chevron is decorative. */
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
private fun ColumnScope.EmptyDigestBody() {
    DigestBody(
        trainingsCount = 0,
        breakdown = persistentListOf(
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.duration),
                value = AppTokens.strings.res(Res.string.digest_zero_duration),
                dimmed = true,
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.tonnage),
                value = VolumeFormatState.shortTotal(0f),
                dimmed = true,
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.sets),
                value = "0",
                dimmed = true,
            ),
        ),
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

/** Emits the panel's hero, divider and breakdown rows; the enclosing panel column owns their rhythm. */
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
