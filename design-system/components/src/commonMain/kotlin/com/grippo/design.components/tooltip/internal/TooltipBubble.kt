package com.grippo.design.components.tooltip.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.grippo.design.components.tooltip.TooltipContent
import com.grippo.design.components.tooltip.TooltipPlacement
import com.grippo.design.components.tooltip.TooltipVariant
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.icons.Warning

@Composable
internal fun TooltipBubble(
    modifier: Modifier = Modifier,
    content: TooltipContent,
    colors: TooltipColorTokens,
    placement: TooltipPlacement,
    arrowPositionPx: Float? = null,
) {
    val dp = AppTokens.dp.tooltip
    val density = LocalDensity.current

    val arrowDepthPx = with(density) { dp.arrowSize.toPx() }
    val arrowBasePx = with(density) { dp.arrowWidth.toPx() }
    val radiusPx = with(density) { dp.radius.toPx() }

    val shape = remember(placement, arrowDepthPx, arrowBasePx, radiusPx, arrowPositionPx) {
        tooltipShape(
            placement = placement,
            arrowDepthPx = arrowDepthPx,
            arrowBasePx = arrowBasePx,
            cornerRadiusPx = radiusPx,
            arrowPositionPx = arrowPositionPx,
        )
    }

    val padStart =
        dp.horizontalPadding + if (placement == TooltipPlacement.End) dp.arrowSize else 0.dp
    val padEnd =
        dp.horizontalPadding + if (placement == TooltipPlacement.Start) dp.arrowSize else 0.dp
    val padTop =
        dp.verticalPadding + if (placement == TooltipPlacement.Bottom) dp.arrowSize else 0.dp
    val padBottom =
        dp.verticalPadding + if (placement == TooltipPlacement.Top) dp.arrowSize else 0.dp

    Box(
        modifier = modifier
            .widthIn(max = dp.maxWidth)
            .shadow(
                elevation = dp.elevation,
                shape = shape,
                ambientColor = AppTokens.colors.overlay.shadow,
                spotColor = AppTokens.colors.overlay.shadow,
            )
            .clip(shape)
            .background(AppTokens.colors.background.dialog)
            .background(colors.background)
            .border(dp.borderWidth, colors.border, shape)
            .padding(start = padStart, end = padEnd, top = padTop, bottom = padBottom),
    ) {
        when (content) {
            is TooltipContent.Simple -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dp.spaceBetween),
                ) {
                    if (content.icon != null) {
                        Icon(
                            imageVector = content.icon,
                            contentDescription = null,
                            tint = colors.icon,
                            modifier = Modifier.size(dp.iconSize),
                        )
                    }
                    Text(
                        text = content.text,
                        style = AppTokens.typography.b13Semi(),
                        color = colors.text,
                    )
                }
            }

            is TooltipContent.Rich -> {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(dp.spaceBetween),
                ) {
                    if (content.icon != null) {
                        Icon(
                            imageVector = content.icon,
                            contentDescription = null,
                            tint = colors.icon,
                            modifier = Modifier.size(dp.iconSize),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.tooltip.textSpacing)) {
                        Text(
                            text = content.title,
                            style = AppTokens.typography.b13Semi(),
                            color = colors.text,
                        )
                        Text(
                            text = content.subtitle,
                            style = AppTokens.typography.b12Med(),
                            color = colors.subtext,
                        )
                    }
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun TooltipBubbleShiftedArrowPreview() {
    PreviewContainer {
        val density = LocalDensity.current
        val nearEdge = with(density) { 8.dp.toPx() }
        val beyondEdge = with(density) { 280.dp.toPx() }
        val colors = resolveTooltipColors(TooltipVariant.Default)

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            TooltipBubble(
                modifier = Modifier.width(240.dp),
                content = TooltipContent.Simple("Anchor near the left edge"),
                colors = colors,
                placement = TooltipPlacement.Top,
                arrowPositionPx = nearEdge,
            )
            TooltipBubble(
                modifier = Modifier.width(240.dp),
                content = TooltipContent.Simple("Anchor beyond the right edge"),
                colors = colors,
                placement = TooltipPlacement.Bottom,
                arrowPositionPx = beyondEdge,
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TooltipBubble(
                    modifier = Modifier.width(240.dp),
                    content = TooltipContent.Rich(
                        title = "RTL start placement",
                        subtitle = "The arrow stays near the top of the physical left edge.",
                    ),
                    colors = colors,
                    placement = TooltipPlacement.Start,
                    arrowPositionPx = nearEdge,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun TooltipBubblePlacementsPreview() {
    PreviewContainer {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            LayoutDirection.entries.forEach { layoutDirection ->
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    listOf(
                        TooltipPlacement.Top to "Top",
                        TooltipPlacement.Bottom to "Bottom",
                        TooltipPlacement.Start to "Start",
                        TooltipPlacement.End to "End",
                    ).forEach { (placement, label) ->
                        TooltipBubble(
                            content = TooltipContent.Simple(text = "$label · $layoutDirection"),
                            colors = resolveTooltipColors(TooltipVariant.Default),
                            placement = placement,
                        )
                    }
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun TooltipBubbleVariantsPreview() {
    PreviewContainer {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(
                TooltipVariant.Default to "Default",
                TooltipVariant.Success to "Success",
                TooltipVariant.Error to "Error",
                TooltipVariant.Warning to "Warning",
                TooltipVariant.Info to "Info",
            ).forEach { (variant, label) ->
                TooltipBubble(
                    content = TooltipContent.Simple(
                        text = label,
                        icon = AppTokens.icons.Warning,
                    ),
                    colors = resolveTooltipColors(variant),
                    placement = TooltipPlacement.Top,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun TooltipBubbleSimpleNoIconPreview() {
    PreviewContainer {
        TooltipBubble(
            content = TooltipContent.Simple(text = "Quick hint, no icon"),
            colors = resolveTooltipColors(TooltipVariant.Default),
            placement = TooltipPlacement.Top,
        )
    }
}

@AppPreview
@Composable
private fun TooltipBubbleRichPreview() {
    PreviewContainer {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TooltipBubble(
                content = TooltipContent.Rich(
                    title = "Personal record!",
                    subtitle = "You lifted 120 kg — your best ever.",
                    icon = AppTokens.icons.Trophy,
                ),
                colors = resolveTooltipColors(TooltipVariant.Success),
                placement = TooltipPlacement.Bottom,
            )
            TooltipBubble(
                content = TooltipContent.Rich(
                    title = "Unusual weight",
                    subtitle = "Possible missing or extra digit.",
                    icon = AppTokens.icons.Warning,
                ),
                colors = resolveTooltipColors(TooltipVariant.Warning),
                placement = TooltipPlacement.Top,
            )
            TooltipBubble(
                content = TooltipContent.Rich(
                    title = "Unusual rep count",
                    subtitle = "Possible missing or extra digit.",
                ),
                colors = resolveTooltipColors(TooltipVariant.Error),
                placement = TooltipPlacement.Start,
            )
        }
    }
}
