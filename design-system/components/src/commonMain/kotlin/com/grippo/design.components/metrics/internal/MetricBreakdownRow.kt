package com.grippo.design.components.metrics.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class MetricBreakdownItem(
    val label: String,
    val value: String,
    val dimmed: Boolean = false,
)

@Composable
internal fun MetricBreakdownRow(
    modifier: Modifier = Modifier,
    items: ImmutableList<MetricBreakdownItem>,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            MetricBreakdownCell(
                modifier = Modifier.weight(1f),
                label = item.label,
                value = item.value,
                dimmed = item.dimmed,
            )

            if (index != items.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(AppTokens.colors.border.default.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
private fun MetricBreakdownCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    dimmed: Boolean,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = value,
            style = AppTokens.typography.h5(),
            color = if (dimmed) {
                AppTokens.colors.text.tertiary
            } else {
                AppTokens.colors.text.primary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Text(
            text = label,
            style = AppTokens.typography.b11Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@AppPreview
@Composable
private fun MetricBreakdownRowPreview() {
    PreviewContainer {
        MetricBreakdownRow(
            items = persistentListOf(
                MetricBreakdownItem(label = "Duration", value = "1:15"),
                MetricBreakdownItem(label = "Sets", value = "12"),
                MetricBreakdownItem(label = "Reps", value = "96"),
                MetricBreakdownItem(label = "Intensity", value = "24 kg"),
            )
        )
    }
}

@AppPreview
@Composable
private fun MetricBreakdownRowDimmedPreview() {
    PreviewContainer {
        MetricBreakdownRow(
            items = persistentListOf(
                MetricBreakdownItem(label = "Duration", value = "0", dimmed = true),
                MetricBreakdownItem(label = "Sets", value = "0", dimmed = true),
                MetricBreakdownItem(label = "Reps", value = "0", dimmed = true),
            )
        )
    }
}
