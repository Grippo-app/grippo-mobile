package com.grippo.design.components.metrics.engagement.digest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.core.state.metrics.engagement.stubDigest
import com.grippo.design.components.metrics.engagement.digest.internal.DigestBreakdown
import com.grippo.design.components.metrics.engagement.digest.internal.DigestBreakdownItem
import com.grippo.design.components.metrics.engagement.digest.internal.DigestFooter
import com.grippo.design.components.metrics.engagement.digest.internal.DigestHeader
import com.grippo.design.components.metrics.engagement.digest.internal.DigestHero
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.modifiers.spot
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.highlight_active_days
import com.grippo.design.resources.provider.highlight_vs_average
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.total
import com.grippo.design.resources.provider.trainings
import com.grippo.design.resources.provider.volume
import com.grippo.toolkit.date.utils.DateRangePresets

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
            is DigestState.Content -> ContentDigestBody(
                value = value,
                accentColor = accentColor,
            )
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

    DigestBreakdown(
        items = listOf(
            DigestBreakdownItem(
                label = AppTokens.strings.res(Res.string.trainings),
                value = "0",
                dimmed = true,
            ),
            DigestBreakdownItem(
                label = AppTokens.strings.res(Res.string.duration),
                value = "0",
                dimmed = true,
            ),
            DigestBreakdownItem(
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
    accentColor: Color,
) {
    DigestHero(
        value = value.total.short(),
        label = AppTokens.strings.res(Res.string.volume),
    )

    DigestBreakdown(
        items = listOf(
            DigestBreakdownItem(
                label = AppTokens.strings.res(Res.string.trainings),
                value = value.trainingsCount.toString(),
            ),
            DigestBreakdownItem(
                label = AppTokens.strings.res(Res.string.duration),
                value = value.duration.display.ifBlank { "0" },
                dimmed = value.duration.display.isBlank(),
            ),
            DigestBreakdownItem(
                label = AppTokens.strings.res(Res.string.sets),
                value = value.totalSets.toString(),
            ),
        )
    )

    val avgVolumeText: String? = if (value.avgVolume is VolumeFormatState.Valid) {
        value.avgVolume.short()
    } else null

    val footerText = buildDigestFooter(
        avgVolume = avgVolumeText,
        activeDays = value.activeDays,
    )

    if (footerText != null) {
        DigestFooter(
            accentColor = accentColor,
            text = footerText,
        )
    }
}

@Composable
private fun buildDigestFooter(
    avgVolume: String?,
    activeDays: Int,
): String? {
    val avgSegment = if (!avgVolume.isNullOrBlank()) {
        val avgLabel = AppTokens.strings.res(Res.string.highlight_vs_average)
        "$avgLabel $avgVolume"
    } else null

    val activeDaysSegment = if (activeDays > 0) {
        AppTokens.strings.res(Res.string.highlight_active_days, activeDays)
    } else null

    val segments = listOfNotNull(avgSegment, activeDaysSegment)
    return segments.takeIf { it.isNotEmpty() }?.joinToString(separator = " · ")
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
