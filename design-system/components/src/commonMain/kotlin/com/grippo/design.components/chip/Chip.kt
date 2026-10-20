package com.grippo.design.components.chip

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.core.state.formatters.UiText
import com.grippo.design.components.chip.internal.resolveChipStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Calendar
import com.grippo.design.resources.provider.icons.Weight

@Stable
public sealed interface ChipLabel {
    @Stable
    public data class Text(val uiText: UiText) : ChipLabel

    @Stable
    public data object Empty : ChipLabel
}

@Stable
public sealed interface ChipSize {
    @Stable
    public data object Small : ChipSize

    @Stable
    public data object Medium : ChipSize
}

@Stable
public sealed interface ChipLeading {
    @Stable
    public data class Content(
        val content: @Composable () -> Unit
    ) : ChipLeading

    @Stable
    public data class Icon(
        val icon: ImageVector,
    ) : ChipLeading

    @Stable
    public data object Empty : ChipLeading
}

@Stable
public sealed interface ChipInteraction {
    @Stable
    public data class Clickable(val onClick: () -> Unit) : ChipInteraction

    @Stable
    public data object Static : ChipInteraction
}

@Composable
public fun Chip(
    modifier: Modifier = Modifier,
    label: ChipLabel,
    value: String,
    interaction: ChipInteraction,
    leading: ChipLeading,
    size: ChipSize,
    textColor: Color,
    iconColor: Color,
    brush: Brush,
    labelColor: Color = textColor,
    borderColor: Color = AppTokens.colors.border.default,
) {
    val tokens = resolveChipStyle(size)
    val shape = RoundedCornerShape(tokens.radius)

    Row(
        modifier = modifier
            .animateContentSize()
            .background(
                brush = brush,
                shape = shape
            )
            .border(
                width = AppTokens.dp.chip.borderWidth,
                color = borderColor,
                shape = shape,
            )
            .let {
                if (interaction is ChipInteraction.Clickable) it.scalableClick(
                    onClick = interaction.onClick,
                    haptic = true
                ) else it
            }
            .heightIn(min = tokens.height)
            .padding(
                horizontal = tokens.horizontalPadding,
                vertical = AppTokens.dp.chip.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = if (label == ChipLabel.Empty) {
                Modifier
            } else {
                Modifier.weight(weight = 1f, fill = false)
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (leading) {
                is ChipLeading.Content -> {
                    Box(modifier = Modifier.size(tokens.leadingSize)) {
                        leading.content()
                    }
                }

                is ChipLeading.Icon -> {
                    Icon(
                        modifier = Modifier.size(tokens.leadingSize),
                        imageVector = leading.icon,
                        contentDescription = null,
                        tint = iconColor
                    )
                }

                ChipLeading.Empty -> Unit
            }
            if (leading != ChipLeading.Empty) {
                Spacer(modifier = Modifier.width(tokens.spaceBetween))
            }

            when (label) {
                is ChipLabel.Text -> {
                    Text(
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .padding(end = AppTokens.dp.contentPadding.text),
                        text = label.uiText.text(),
                        style = tokens.labelTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = labelColor
                    )
                }

                ChipLabel.Empty -> Unit
            }
        }

        Text(
            text = value,
            style = tokens.valueTextStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = textColor
        )
    }
}

@AppPreview
@Composable
private fun ChipPreview() {
    PreviewContainer {
        Chip(
            modifier = Modifier.fillMaxWidth(),
            label = ChipLabel.Text(UiText.Str(value = "Label label label")),
            value = "Value",
            leading = ChipLeading.Icon(AppTokens.icons.Weight),
            brush = SolidColor(AppTokens.colors.background.card),
            textColor = AppTokens.colors.text.primary,
            labelColor = AppTokens.colors.text.secondary,
            iconColor = AppTokens.colors.text.primary,
            interaction = ChipInteraction.Static,
            size = ChipSize.Medium
        )

        Chip(
            label = ChipLabel.Text(UiText.Str(value = "Label label label")),
            value = "Value",
            leading = ChipLeading.Icon(AppTokens.icons.Weight),
            brush = SolidColor(AppTokens.colors.background.card),
            textColor = AppTokens.colors.text.primary,
            labelColor = AppTokens.colors.text.secondary,
            iconColor = AppTokens.colors.text.primary,
            interaction = ChipInteraction.Static,
            size = ChipSize.Medium
        )

        Chip(
            modifier = Modifier.width(150.dp),
            label = ChipLabel.Text(UiText.Str(value = "Label Label Label")),
            value = "value",
            leading = ChipLeading.Icon(AppTokens.icons.Weight),
            brush = SolidColor(AppTokens.colors.background.card),
            textColor = AppTokens.colors.text.primary,
            labelColor = AppTokens.colors.text.secondary,
            iconColor = AppTokens.colors.text.primary,
            interaction = ChipInteraction.Static,
            size = ChipSize.Medium
        )

        Chip(
            modifier = Modifier.fillMaxWidth(),
            label = ChipLabel.Text(UiText.Str(value = "Label label label")),
            value = "Value",
            leading = ChipLeading.Icon(AppTokens.icons.Weight),
            brush = SolidColor(AppTokens.colors.background.card),
            textColor = AppTokens.colors.text.primary,
            labelColor = AppTokens.colors.text.secondary,
            iconColor = AppTokens.colors.text.primary,
            interaction = ChipInteraction.Static,
            size = ChipSize.Small
        )

        Chip(
            label = ChipLabel.Text(UiText.Str(value = "Label label label")),
            value = "Value",
            leading = ChipLeading.Icon(AppTokens.icons.Weight),
            brush = SolidColor(AppTokens.colors.background.card),
            textColor = AppTokens.colors.text.primary,
            labelColor = AppTokens.colors.text.secondary,
            iconColor = AppTokens.colors.text.primary,
            interaction = ChipInteraction.Static,
            size = ChipSize.Small
        )

        Chip(
            modifier = Modifier.width(100.dp),
            label = ChipLabel.Text(UiText.Str(value = "Label Label Label")),
            value = "value",
            leading = ChipLeading.Icon(AppTokens.icons.Weight),
            brush = SolidColor(AppTokens.colors.background.card),
            textColor = AppTokens.colors.text.primary,
            labelColor = AppTokens.colors.text.secondary,
            iconColor = AppTokens.colors.text.primary,
            interaction = ChipInteraction.Static,
            size = ChipSize.Small
        )
    }
}

@AppPreview
@Composable
private fun ChipContentVariantsPreview() {
    PreviewContainer {
        Chip(
            label = ChipLabel.Empty,
            value = "12 500 kg",
            leading = ChipLeading.Icon(AppTokens.icons.Weight),
            brush = SolidColor(AppTokens.colors.background.card),
            textColor = AppTokens.colors.text.primary,
            iconColor = AppTokens.colors.context.volume,
            interaction = ChipInteraction.Static,
            size = ChipSize.Small,
        )

        Chip(
            label = ChipLabel.Text(UiText.Str("Supporting label")),
            value = "Value",
            leading = ChipLeading.Empty,
            brush = SolidColor(AppTokens.colors.background.card),
            textColor = AppTokens.colors.text.primary,
            labelColor = AppTokens.colors.text.secondary,
            iconColor = AppTokens.colors.icon.secondary,
            interaction = ChipInteraction.Static,
            size = ChipSize.Medium,
        )

        Chip(
            label = ChipLabel.Empty,
            value = "12",
            leading = ChipLeading.Empty,
            brush = SolidColor(AppTokens.colors.selection.background),
            textColor = AppTokens.colors.text.primary,
            iconColor = AppTokens.colors.icon.secondary,
            borderColor = AppTokens.colors.selection.accent,
            interaction = ChipInteraction.Clickable(onClick = {}),
            size = ChipSize.Medium,
        )
    }
}

@AppPreview
@Composable
private fun ChipTwoLineValuePreview() {
    PreviewContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            Spacer(modifier = Modifier.size(AppTokens.dp.exerciseExampleImage.medium.size))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                Chip(
                    label = ChipLabel.Empty,
                    value = "Последнее использование: 14 октября 2026 года",
                    leading = ChipLeading.Icon(AppTokens.icons.Calendar),
                    brush = SolidColor(AppTokens.colors.background.card),
                    textColor = AppTokens.colors.text.primary,
                    iconColor = AppTokens.colors.icon.secondary,
                    interaction = ChipInteraction.Static,
                    size = ChipSize.Small,
                )
                Chip(
                    label = ChipLabel.Empty,
                    value = "Останнє використання: 14 жовтня 2026 року",
                    leading = ChipLeading.Icon(AppTokens.icons.Calendar),
                    brush = SolidColor(AppTokens.colors.background.card),
                    textColor = AppTokens.colors.text.primary,
                    iconColor = AppTokens.colors.icon.secondary,
                    interaction = ChipInteraction.Static,
                    size = ChipSize.Small,
                )
                Chip(
                    label = ChipLabel.Text(UiText.Str(value = "History")),
                    value = "Останнє використання: 14 жовтня 2026 року",
                    leading = ChipLeading.Icon(AppTokens.icons.Calendar),
                    brush = SolidColor(AppTokens.colors.background.card),
                    textColor = AppTokens.colors.text.primary,
                    labelColor = AppTokens.colors.text.secondary,
                    iconColor = AppTokens.colors.icon.secondary,
                    interaction = ChipInteraction.Static,
                    size = ChipSize.Small,
                )
            }
        }
    }
}
