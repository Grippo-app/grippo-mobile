package com.grippo.design.components.metrics.engagement.digest.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Trophy

@Composable
internal fun DigestHeader(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.engagement.digest.icon),
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
        )

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun DigestHero(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    dimmed: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = value,
            style = AppTokens.typography.h3(),
            color = if (dimmed) {
                AppTokens.colors.text.tertiary
            } else {
                AppTokens.colors.text.primary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = label,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@AppPreview
@Composable
private fun DigestsSectionPreview() {
    PreviewContainer {
        DigestHeader(
            icon = AppTokens.icons.Trophy,
            accentColor = AppTokens.colors.brand.color6,
            title = "Total",
            subtitle = "Apr 2026",
        )

        DigestHero(
            value = "3 250 kg",
            label = "Volume",
        )
    }
}
