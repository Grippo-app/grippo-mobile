package com.grippo.design.components.metrics.engagement.digest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.design.components.metrics.engagement.digest.internal.DigestHeader
import com.grippo.design.components.metrics.engagement.digest.internal.DigestHero
import com.grippo.design.components.metrics.internal.MetricBreakdownItem
import com.grippo.design.components.metrics.internal.MetricBreakdownRow
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.total
import com.grippo.design.resources.provider.trainings
import com.grippo.design.resources.provider.volume
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
public fun DigestCard(
    modifier: Modifier = Modifier,
    value: DigestState,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
    ) {
        DigestHeader(
            icon = AppTokens.icons.Trophy,
            iconColor = AppTokens.colors.context.volume,
            title = AppTokens.strings.res(Res.string.total),
            subtitle = value.range.display,
        )

        when (value) {
            is DigestState.Empty -> EmptyDigestBody()
            is DigestState.Content -> ContentDigestBody(value = value)
        }
    }
}

@Composable
private fun EmptyDigestBody() {
    val kgSuffix = AppTokens.strings.res(Res.string.kg)

    DigestHero(
        value = "0$kgSuffix",
        label = AppTokens.strings.res(Res.string.volume),
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
                value = "0",
                dimmed = true,
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.sets),
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
    val duration = value.duration.short()

    DigestHero(
        value = value.total.short(),
        label = AppTokens.strings.res(Res.string.volume),
    )

    MetricBreakdownRow(
        items = persistentListOf(
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.trainings),
                value = value.trainingsCount.toString(),
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.duration),
                value = duration.ifBlank { "0" },
                dimmed = duration.isBlank(),
            ),
            MetricBreakdownItem(
                label = AppTokens.strings.res(Res.string.sets),
                value = value.totalSets.toString(),
            ),
        )
    )
}

@AppPreview
@Composable
private fun DigestCardPreview() {
    PreviewContainer {
        DigestCard(
            value = digestPreviewValue(),
        )
    }
}

@AppPreview
@Composable
private fun DigestCardEmptyPreview() {
    PreviewContainer {
        DigestCard(
            value = DigestState.Empty(
                range = digestPreviewValue().range,
            ),
        )
    }
}

@AppPreview
@Composable
private fun DigestCardLongValuesPreview() {
    PreviewContainer {
        DigestCard(
            value = digestPreviewValue().copy(
                duration = DurationFormatState.of(123.hours + 59.minutes),
                total = VolumeFormatState.of(184_829.5f),
                totalSets = 12_345,
                trainingsCount = 123,
            ),
        )
    }
}

private fun digestPreviewValue(): DigestState.Content = DigestState.Content(
    range = DateRangeFormatState.of(
        from = LocalDateTime(2026, 10, 1, 0, 0),
        to = LocalDateTime(2026, 10, 31, 23, 59),
    ),
    trainingsCount = 5,
    duration = DurationFormatState.of(7.hours),
    total = VolumeFormatState.of(540f),
    avgVolume = VolumeFormatState.of(108f),
    totalSets = 32,
    activeDays = 4,
)
