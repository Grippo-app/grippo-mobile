package com.grippo.design.components.metrics.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_title
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.metric_overview_muscle_distribution

/** The parent card owns the detail action; the chevron is decorative. */
@Composable
internal fun MetricOverviewHeader(
    modifier: Modifier = Modifier,
    title: String,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
        )
        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.overview.detailsIcon),
            imageVector = AppTokens.icons.ArrowRight,
            tint = AppTokens.colors.icon.secondary,
            contentDescription = null,
        )
    }
}

@AppPreview
@Composable
private fun MetricOverviewHeaderPreview() {
    PreviewContainer {
        MetricSectionPanel(style = MetricSectionPanelStyle.Large) {
            MetricOverviewHeader(title = AppTokens.strings.res(Res.string.goal_card_title))
        }
        MetricSectionPanel(style = MetricSectionPanelStyle.Large) {
            MetricOverviewHeader(title = AppTokens.strings.res(Res.string.metric_overview_muscle_distribution))
        }
    }
}
