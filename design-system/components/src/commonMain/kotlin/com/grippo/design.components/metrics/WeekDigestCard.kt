package com.grippo.design.components.metrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.digest.WeeklyDigestState
import com.grippo.core.state.metrics.digest.stubWeeklyDigest
import com.grippo.design.components.metrics.internal.DigestCardStyle
import com.grippo.design.components.metrics.internal.DigestMetric
import com.grippo.design.components.metrics.internal.DigestsSection
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.trainings
import com.grippo.design.resources.provider.volume
import com.grippo.design.resources.provider.weekly
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun WeekDigestCard(
    modifier: Modifier = Modifier,
    value: WeeklyDigestState,
) {
    val subtitle = remember(value.weekStart, value.weekEnd) {
        val from = DateTimeUtils.format(value.weekStart, DateFormat.DateOnly.DateDdMmm)
        val to = DateTimeUtils.format(value.weekEnd, DateFormat.DateOnly.DateDdMmm)
        "$from - $to"
    }

    val style = DigestCardStyle(
        radius = AppTokens.dp.digest.week.radius,
        horizontalPadding = AppTokens.dp.digest.week.horizontalPadding,
        verticalPadding = AppTokens.dp.digest.week.verticalPadding,
        iconSize = AppTokens.dp.digest.week.icon,
        illustrationSize = AppTokens.dp.digest.week.image,
    )

    DigestsSection(
        modifier = modifier,
        style = style,
        icon = AppTokens.icons.Trophy,
        accentColor = AppTokens.colors.brand.color4,
        title = AppTokens.strings.res(Res.string.weekly),
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
private fun WeekDigestCardPreview() {
    PreviewContainer {
        WeekDigestCard(
            value = stubWeeklyDigest(),
        )
    }
}
