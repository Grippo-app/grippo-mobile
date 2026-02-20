package com.grippo.design.components.chip

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.core.state.formatters.UiText
import com.grippo.design.components.chip.internal.resolveChipStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
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
public sealed interface ChipTrailing {
    @Stable
    public data class Content(
        val lambda: @Composable () -> Unit
    ) : ChipTrailing

    @Stable
    public data class Icon(
        val icon: ImageVector,
    ) : ChipTrailing

    @Stable
    public data object Empty : ChipTrailing
}

@Stable
public sealed interface ChipStype {
    @Stable
    public data class Clickable(val onClick: () -> Unit) : ChipStype

    @Stable
    public data object Default : ChipStype
}

@Composable
public fun Chip(
    modifier: Modifier = Modifier,
    label: ChipLabel,
    value: String,
    stype: ChipStype,
    trailing: ChipTrailing,
    size: ChipSize,
    textColor: Color,
    iconColor: Color,
    brush: Brush,
) {
    val tokens = resolveChipStyle(size)

    val shape = RoundedCornerShape(tokens.radius)

    Row(
        modifier = modifier
            .animateContentSize()
            .background(
                brush = brush,
                shape = shape
            ).let {
                if (stype is ChipStype.Clickable) it.scalableClick(onClick = stype.onClick) else it
            }.padding(
                horizontal = tokens.horizontalPadding,
            ).height(
                height = tokens.height
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(weight = 1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (trailing) {
                is ChipTrailing.Content -> {
                    Box(modifier = Modifier.size(tokens.trailingSize)) {
                        trailing.lambda.invoke()
                    }
                }

                is ChipTrailing.Icon -> {
                    Icon(
                        modifier = Modifier.size(tokens.trailingSize),
                        imageVector = trailing.icon,
                        contentDescription = null,
                        tint = iconColor
                    )
                }

                ChipTrailing.Empty -> Unit
            }
            if (trailing != ChipTrailing.Empty) {
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
                        color = textColor
                    )
                }

                ChipLabel.Empty -> Unit
            }
        }

        Text(
            modifier = Modifier.padding(end = AppTokens.dp.contentPadding.text),
            text = value,
            style = tokens.valueTextStyle,
            maxLines = 1,
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
            trailing = ChipTrailing.Icon(AppTokens.icons.Weight),
            brush = Brush.linearGradient(listOf(Color.Gray, Color.LightGray)),
            textColor = Color.Black,
            iconColor = Color.Black,
            stype = ChipStype.Default,
            size = ChipSize.Medium
        )

        Chip(
            label = ChipLabel.Text(UiText.Str(value = "Label label label")),
            value = "Value",
            trailing = ChipTrailing.Icon(AppTokens.icons.Weight),
            brush = Brush.linearGradient(listOf(Color.Gray, Color.LightGray)),
            textColor = Color.Black,
            iconColor = Color.Black,
            stype = ChipStype.Default,
            size = ChipSize.Medium
        )

        Chip(
            modifier = Modifier.width(150.dp),
            label = ChipLabel.Text(UiText.Str(value = "Label Label Label")),
            value = "value",
            trailing = ChipTrailing.Icon(AppTokens.icons.Weight),
            brush = Brush.linearGradient(listOf(Color.Gray, Color.LightGray)),
            textColor = Color.Black,
            iconColor = Color.Black,
            stype = ChipStype.Default,
            size = ChipSize.Medium
        )

        Chip(
            modifier = Modifier.fillMaxWidth(),
            label = ChipLabel.Text(UiText.Str(value = "Label label label")),
            value = "Value",
            trailing = ChipTrailing.Icon(AppTokens.icons.Weight),
            brush = Brush.linearGradient(listOf(Color.Gray, Color.LightGray)),
            textColor = Color.Black,
            iconColor = Color.Black,
            stype = ChipStype.Default,
            size = ChipSize.Small
        )

        Chip(
            label = ChipLabel.Text(UiText.Str(value = "Label label label")),
            value = "Value",
            trailing = ChipTrailing.Icon(AppTokens.icons.Weight),
            brush = Brush.linearGradient(listOf(Color.Gray, Color.LightGray)),
            textColor = Color.Black,
            iconColor = Color.Black,
            stype = ChipStype.Default,
            size = ChipSize.Small
        )

        Chip(
            modifier = Modifier.width(100.dp),
            label = ChipLabel.Text(UiText.Str(value = "Label Label Label")),
            value = "value",
            trailing = ChipTrailing.Icon(AppTokens.icons.Weight),
            brush = Brush.linearGradient(listOf(Color.Gray, Color.LightGray)),
            textColor = Color.Black,
            iconColor = Color.Black,
            stype = ChipStype.Default,
            size = ChipSize.Small
        )
    }
}
