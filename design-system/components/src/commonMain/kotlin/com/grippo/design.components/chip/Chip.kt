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
import com.grippo.design.core.AppTokens
import com.grippo.design.core.UiText
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.Weight

@Stable
public sealed interface Trailing {
    @Stable
    public data class Content(
        val lambda: @Composable () -> Unit
    ) : Trailing

    @Stable
    public data class Icon(
        val icon: ImageVector,
    ) : Trailing

    @Stable
    public data object Empty : Trailing
}

@Stable
public sealed interface Label {
    @Stable
    public data class Text(val uiText: UiText) : Label

    @Stable
    public data object Empty : Label
}

@Composable
public fun Chip(
    modifier: Modifier = Modifier,
    label: Label,
    value: String,
    trailing: Trailing,
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
            .padding(
                horizontal = AppTokens.dp.chip.horizontalPadding,
                vertical = AppTokens.dp.chip.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            when (trailing) {
                is Trailing.Content -> {
                    Box(modifier = Modifier.size(AppTokens.dp.chip.trailingSize)) {
                        trailing.lambda.invoke()
                    }
                    Spacer(modifier = Modifier.width(AppTokens.dp.chip.spaceBetween))
                }

                is Trailing.Icon -> {
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

                Trailing.Empty -> {
                    // Empty
                }
            }

            when (label) {
                is Label.Text -> {
                    Text(
                        text = label.uiText.text(),
                        style = AppTokens.typography.b14Semi(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(AppTokens.dp.contentPadding.text))
                }

                Label.Empty -> {
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
            label = Label.Text(UiText.Str(value = "Basic")),
            value = "Value",
            trailing = Trailing.Icon(AppTokens.icons.Weight),
            brush = Brush.linearGradient(listOf(Color.Gray, Color.LightGray)),
            contentColor = Color.Black
        )
    }
}
