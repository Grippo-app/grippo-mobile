package com.grippo.design.components.metrics.engagement.digest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.core.state.metrics.engagement.stubDigest
import com.grippo.design.components.metrics.engagement.digest.internal.DigestHeader
import com.grippo.design.components.metrics.engagement.digest.internal.DigestVolume
import com.grippo.design.components.metrics.internal.MetricBreakdownItem
import com.grippo.design.components.metrics.internal.MetricBreakdownRow
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.digest_active_days
import com.grippo.design.resources.provider.digest_title
import com.grippo.design.resources.provider.digest_total_volume
import com.grippo.design.resources.provider.digest_unavailable
import com.grippo.design.resources.provider.digest_volume_per_workout
import com.grippo.design.resources.provider.digest_zero_duration
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.available_sets_label
import com.grippo.design.resources.provider.trainings
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
public fun DigestCard(
    modifier: Modifier = Modifier,
    value: DigestState,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        DigestHeader(
            icon = AppTokens.icons.Trophy,
            iconColor = AppTokens.colors.context.volume,
            title = AppTokens.strings.res(Res.string.digest_title),
            subtitle = value.range.short(),
        )

        when (value) {
            is DigestState.Empty -> EmptyDigestBody()
            is DigestState.Content -> ContentDigestBody(value = value)
        }
    }
}

@Composable
private fun EmptyDigestBody() {
    DigestVolume(
        value = VolumeFormatState.shortTotal(0f),
        label = AppTokens.strings.res(Res.string.digest_total_volume),
        dimmed = true,
    )

    MetricBreakdownRow(
        items = persistentListOf(
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.trainings),
                value = "0",
                dimmed = true,
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.duration),
                value = AppTokens.strings.res(Res.string.digest_zero_duration),
                dimmed = true,
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.available_sets_label),
                value = "0",
                dimmed = true,
            ),
        )
    )
}

@Composable
private fun ContentDigestBody(
    value: DigestState.Content,
) {
    val duration = when (value.duration.value) {
        null -> AppTokens.strings.res(Res.string.digest_unavailable)
        Duration.ZERO -> AppTokens.strings.res(Res.string.digest_zero_duration)
        else -> value.duration.short()
    }

    DigestVolume(
        value = VolumeFormatState.shortTotal(value.total.value),
        label = AppTokens.strings.res(Res.string.digest_total_volume),
    )

    MetricBreakdownRow(
        items = persistentListOf(
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.trainings),
                value = value.trainingsCount.toString(),
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.duration),
                value = duration,
                dimmed = value.duration.value == null,
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.available_sets_label),
                value = value.totalSets.toString(),
            ),
        )
    )

    MetricBreakdownRow(
        items = persistentListOf(
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.digest_active_days),
                value = value.activeDays.toString(),
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.digest_volume_per_workout),
                value = VolumeFormatState.shortTotal(value.avgVolume.value),
            ),
        )
    )
}

@AppPreview
@Composable
private fun DigestCardPreview() {
    PreviewContainer {
        DigestCard(
            value = stubDigest(),
        )
    }
}

@AppPreview
@Composable
private fun DigestCardEmptyPreview() {
    PreviewContainer {
        DigestCard(
            value = DigestState.Empty(
                range = stubDigest().range,
            ),
        )
    }
}

@AppPreview
@Composable
private fun DigestCardMissingDurationPreview() {
    PreviewContainer {
        DigestCard(
            value = stubDigest().copy(duration = DurationFormatState.Empty()),
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
                avgVolume = VolumeFormatState.ofTotal(0f),
            ),
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
                avgVolume = VolumeFormatState.ofTotal(1_502.7f),
                totalSets = 12_345,
                trainingsCount = 123,
                activeDays = 31,
            ),
        )
    }
}
