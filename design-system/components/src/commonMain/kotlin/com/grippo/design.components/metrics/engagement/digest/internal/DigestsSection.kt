package com.grippo.design.components.metrics.engagement.digest.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens

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

@Composable
internal fun DigestBreakdown(
    modifier: Modifier = Modifier,
    items: List<DigestBreakdownItem>,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            DigestBreakdownCell(
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
private fun DigestBreakdownCell(
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

@Composable
internal fun DigestFooter(
    modifier: Modifier = Modifier,
    accentColor: Color,
    text: String,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accentColor)
        )

        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal data class DigestBreakdownItem(
    val label: String,
    val value: String,
    val dimmed: Boolean = false,
)
