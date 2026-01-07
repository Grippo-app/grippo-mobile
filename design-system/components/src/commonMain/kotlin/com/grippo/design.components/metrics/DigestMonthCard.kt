package com.grippo.design.components.metrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.MonthlyDigestState
import com.grippo.core.state.metrics.stubMonthlyDigest
import com.grippo.design.components.metrics.internal.DigestCardStyle
import com.grippo.design.components.metrics.internal.DigestMetric
import com.grippo.design.components.metrics.internal.DigestsSection
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.monthly
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.trainings
import com.grippo.design.resources.provider.volume
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun DigestMonthCard(
    modifier: Modifier = Modifier,
    value: MonthlyDigestState,
) {
    val subtitle = remember(value.month) {
        val monthTitle = DateTimeUtils.format(value.month, DateFormat.DateOnly.MonthFull)
        "$monthTitle ${value.month.year}"
    }

    val style = DigestCardStyle(
        radius = AppTokens.dp.digest.month.radius,
        horizontalPadding = AppTokens.dp.digest.month.horizontalPadding,
        verticalPadding = AppTokens.dp.digest.month.verticalPadding,
        iconSize = AppTokens.dp.digest.month.icon,
    )

    DigestsSection(
        modifier = modifier,
        style = style,
        icon = AppTokens.icons.Trophy,
        accentColor = AppTokens.colors.brand.color6,
        title = AppTokens.strings.res(Res.string.monthly),
        subtitle = subtitle,
        metrics = listOf(
            DigestMetric(
                label = AppTokens.strings.res(Res.string.trainings),
                value = value.trainingsCount.toString(),
            ),
            DigestMetric(
                label = AppTokens.strings.res(Res.string.sets),
                value = value.totalSets.toString(),
            ),
            DigestMetric(
                label = AppTokens.strings.res(Res.string.duration),
                value = DateTimeUtils.format(value.duration)
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
private fun DigestMonthCardPreview() {
    PreviewContainer {
        DigestMonthCard(
            value = stubMonthlyDigest(),
        )
    }
}
