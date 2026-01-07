package com.grippo.design.components.metrics.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public enum class MetricSectionPanelStyle {
    Small,
    Large
}

@Composable
internal fun MetricSectionPanel(
    modifier: Modifier = Modifier,
    style: MetricSectionPanelStyle,
    decoration: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {

    val radius = when (style) {
        MetricSectionPanelStyle.Small -> AppTokens.dp.metrics.panel.small.radius
        MetricSectionPanelStyle.Large -> AppTokens.dp.metrics.panel.large.radius
    }

    val horizontalPadding = when (style) {
        MetricSectionPanelStyle.Small -> AppTokens.dp.metrics.panel.small.horizontalPadding
        MetricSectionPanelStyle.Large -> AppTokens.dp.metrics.panel.large.horizontalPadding
    }

    val verticalPadding = when (style) {
        MetricSectionPanelStyle.Small -> AppTokens.dp.metrics.panel.small.verticalPadding
        MetricSectionPanelStyle.Large -> AppTokens.dp.metrics.panel.large.verticalPadding
    }

    val spacer = when (style) {
        MetricSectionPanelStyle.Small -> AppTokens.dp.metrics.panel.small.spacer
        MetricSectionPanelStyle.Large -> AppTokens.dp.metrics.panel.large.spacer
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .height(intrinsicSize = IntrinsicSize.Min)
            .background(
                AppTokens.colors.background.card,
                shape = RoundedCornerShape(radius)
            )
    ) {
        decoration?.invoke(this)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(spacer),
            content = content
        )
    }
}

@AppPreview
@Composable
private fun MetricSectionPanelBigPreview() {
    PreviewContainer {
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Large
        ) {
            Text(
                text = "Metric section title",
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary
            )

            Text(
                text = "Primary metric value",
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary
            )
        }
    }
}

@AppPreview
@Composable
private fun MetricSectionPanelSmallPreview() {
    PreviewContainer {
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Small
        ) {
            Text(
                text = "Metric section title",
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary
            )

            Text(
                text = "Primary metric value",
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary
            )
        }
    }
}
