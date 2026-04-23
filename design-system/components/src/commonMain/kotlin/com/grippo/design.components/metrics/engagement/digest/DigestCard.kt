package com.grippo.design.components.metrics.engagement.digest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.core.state.metrics.engagement.stubDigest
import com.grippo.design.components.metrics.engagement.digest.internal.DigestCardStyle
import com.grippo.design.components.metrics.engagement.digest.internal.DigestMetric
import com.grippo.design.components.metrics.engagement.digest.internal.DigestsSection
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.total
import com.grippo.design.resources.provider.trainings
import com.grippo.design.resources.provider.volume

@Composable
public fun DigestCard(
    modifier: Modifier = Modifier,
    value: DigestState,
) {
    val style = DigestCardStyle(
        radius = AppTokens.dp.metrics.engagement.digest.content.radius,
        horizontalPadding = AppTokens.dp.metrics.engagement.digest.content.horizontalPadding,
        verticalPadding = AppTokens.dp.metrics.engagement.digest.content.verticalPadding,
        iconSize = AppTokens.dp.metrics.engagement.digest.content.icon,
    )

    DigestsSection(
        modifier = modifier,
        style = style,
        icon = AppTokens.icons.Trophy,
        accentColor = AppTokens.colors.brand.color6,
        title = AppTokens.strings.res(Res.string.total),
        subtitle = value.range.display,
        metrics = listOf(
            DigestMetric(
                label = AppTokens.strings.res(Res.string.trainings),
                value = value.trainingsCount.toString(),
            ),
            DigestMetric(
                label = AppTokens.strings.res(Res.string.duration),
                value = value.duration.display
            ),
            DigestMetric(
                label = AppTokens.strings.res(Res.string.volume),
                value = value.total.short()
            )
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
