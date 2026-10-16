package com.grippo.design.components.metrics.engagement.digest.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Trophy

@Composable
internal fun DigestHeader(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
) {
    val dp = AppTokens.dp.metrics.engagement.digest

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(dp.iconContainer)
                .background(
                    color = AppTokens.colors.background.screen,
                    shape = RoundedCornerShape(dp.iconRadius),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(dp.icon),
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
            )
        }

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
        )

        if (subtitle.isNotBlank()) {
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = subtitle,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.tertiary,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
internal fun DigestVolume(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    dimmed: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            style = AppTokens.typography.h4(),
            color = if (dimmed) {
                AppTokens.colors.text.tertiary
            } else {
                AppTokens.colors.text.primary
            },
        )

        Text(
            text = label,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.End,
        )
    }
}

@AppPreview
@Composable
private fun DigestsSectionPreview() {
    PreviewContainer {
        DigestHeader(
            icon = AppTokens.icons.Trophy,
            iconColor = AppTokens.colors.context.volume,
            title = "Total",
            subtitle = "Apr 2026",
        )

        DigestVolume(
            value = "3 250 kg",
            label = "Volume",
        )
    }
}

@AppPreview
@Composable
private fun DigestHeaderLongDatePreview() {
    PreviewContainer {
        DigestHeader(
            icon = AppTokens.icons.Trophy,
            iconColor = AppTokens.colors.context.volume,
            title = "Training summary",
            subtitle = "September 15 – October 15, 2026",
        )
        DigestHeader(
            icon = AppTokens.icons.Trophy,
            iconColor = AppTokens.colors.context.volume,
            title = "Training summary",
            subtitle = "",
        )
    }
}
