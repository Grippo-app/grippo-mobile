package com.grippo.design.components.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Weight
import com.grippo.state.formatters.UiText

@Stable
public sealed interface ChipLabel {
    @Stable
    public data class Text(val uiText: UiText) : ChipLabel

    @Stable
    public data object Empty : ChipLabel
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
    contentColor: Color,
    brush: Brush,
) {
    val shape = RoundedCornerShape(AppTokens.dp.chip.radius)

    Row(
        modifier = modifier
            .background(
                brush = brush,
                shape = shape
            )
            .let {
                if (stype is ChipStype.Clickable) {
                    it.scalableClick(onClick = stype.onClick)
                } else {
                    it
                }
            }
            .padding(
                horizontal = AppTokens.dp.chip.horizontalPadding,
                vertical = AppTokens.dp.chip.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            when (trailing) {
                is ChipTrailing.Content -> {
                    Box(modifier = Modifier.size(AppTokens.dp.chip.trailingSize)) {
                        trailing.lambda.invoke()
                    }
                    Spacer(modifier = Modifier.width(AppTokens.dp.chip.spaceBetween))
                }

                is ChipTrailing.Icon -> {
                    Box(modifier = Modifier.size(AppTokens.dp.chip.trailingSize)) {
                        Icon(
                            modifier = Modifier.size(AppTokens.dp.chip.trailingSize),
                            imageVector = trailing.icon,
                            contentDescription = null,
                            tint = contentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(AppTokens.dp.chip.spaceBetween))
                }

                ChipTrailing.Empty -> {
                    // Empty
                }
            }

            when (label) {
                is ChipLabel.Text -> {
                    Text(
                        text = label.uiText.text(),
                        style = AppTokens.typography.b14Semi(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(AppTokens.dp.contentPadding.text))
                }

                ChipLabel.Empty -> {
                    // Empty
                }
            }
        }

        Text(
            modifier = Modifier,
            text = value,
            style = AppTokens.typography.b14Bold(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor
        )
    }
}

@AppPreview
@Composable
private fun ChipPreview() {
    PreviewContainer {
        Chip(
            label = ChipLabel.Text(UiText.Str(value = "Basic")),
            value = "Value",
            trailing = ChipTrailing.Icon(AppTokens.icons.Weight),
            brush = Brush.linearGradient(listOf(Color.Gray, Color.LightGray)),
            contentColor = Color.Black,
            stype = ChipStype.Default
        )
    }
}
