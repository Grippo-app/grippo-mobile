package com.grippo.design.components.metrics.engagement.digest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.core.state.metrics.engagement.stubDigest
import com.grippo.design.components.metrics.engagement.digest.internal.DigestHeader
import com.grippo.design.components.metrics.engagement.digest.internal.DigestHero
import com.grippo.design.components.metrics.internal.MetricBreakdownItem
import com.grippo.design.components.metrics.internal.MetricBreakdownRow
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.modifiers.spot
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
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun DigestCard(
    modifier: Modifier = Modifier,
    value: DigestState,
) {
    val accentColor = AppTokens.colors.brand.color6

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
        decorator = {
            Box(
                modifier = Modifier
                    .spot(color = accentColor, alpha = 0.18f)
                    .align(Alignment.TopEnd)
                    .size(AppTokens.dp.metrics.trainingSummary.spot)
                    .scale(1.4f),
            )
        }
    ) {
        DigestHeader(
            icon = AppTokens.icons.Trophy,
            accentColor = accentColor,
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
                value = value.duration.display.ifBlank { "0" },
                dimmed = value.duration.display.isBlank(),
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
                range = DateRangeFormatState.of(DateRangePresets.monthly()),
            ),
        )
    }
}
