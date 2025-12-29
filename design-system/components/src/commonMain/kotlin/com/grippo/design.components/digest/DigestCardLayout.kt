package com.grippo.design.components.digest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.grippo.design.components.modifiers.spot
import com.grippo.design.core.AppTokens

@Composable
internal fun DigestCard(
    modifier: Modifier = Modifier,
    style: DigestCardStyle,
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    metrics: List<DigestMetric>,
) {
    Box(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Max)
            .clip(RoundedCornerShape(style.radius))
            .background(
                AppTokens.colors.background.card,
                shape = RoundedCornerShape(style.radius)
            )
    ) {
        Icon(
            modifier = Modifier
                .spot(color = accentColor)
                .align(Alignment.CenterEnd)
                .offset(x = (style.illustrationSize / 2))
                .size(style.illustrationSize)
                .alpha(0.15f),
            imageVector = icon,
            contentDescription = null,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = style.horizontalPadding,
                    vertical = style.verticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            DigestCardHeader(
                icon = icon,
                accentColor = accentColor,
                title = title,
                subtitle = subtitle,
                style = style
            )

            DigestMetricsGrid(
                metrics = metrics,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun DigestCardHeader(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    style: DigestCardStyle,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(style.iconSize),
            imageVector = icon,
            contentDescription = null,
            tint = accentColor
        )

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTokens.typography.h4(),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (subtitle.isNotBlank()) {
            DigestSubtitleChip(
                text = subtitle,
                accent = accentColor
            )
        }
    }
}

@Composable
private fun DigestSubtitleChip(
    text: String,
    accent: Color,
) {
    val chipShape = RoundedCornerShape(AppTokens.dp.digest.layout.radius)

    Text(
        modifier = Modifier
            .clip(chipShape)
            .background(
                color = accent.copy(alpha = 0.12f),
                shape = chipShape
            )
            .padding(
                horizontal = AppTokens.dp.digest.layout.horizontalPadding,
                vertical = AppTokens.dp.digest.layout.verticalPadding
            ),
        text = text,
        style = AppTokens.typography.b12Semi(),
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun DigestMetricsGrid(
    metrics: List<DigestMetric>,
    accentColor: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        metrics
            .chunked(2)
            .forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
                ) {
                    rowItems.forEach { metric ->
                        DigestMetricPanel(
                            modifier = Modifier.weight(1f),
                            label = metric.label,
                            value = metric.value,
                            accentColor = accentColor
                        )
                    }

                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
    }
}

@Composable
private fun DigestMetricPanel(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
) {
    val shape = RoundedCornerShape(AppTokens.dp.home.highlights.panel.radius)

    Column(
        modifier = modifier
            .clip(shape)
            .background(
                color = accentColor.copy(alpha = 0.08f),
                shape = shape
            )
            .padding(AppTokens.dp.contentPadding.content),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        Text(
            text = label,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = value,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal data class DigestMetric(
    val label: String,
    val value: String,
)

internal data class DigestCardStyle(
    val radius: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val iconSize: Dp,
    val illustrationSize: Dp,
)
