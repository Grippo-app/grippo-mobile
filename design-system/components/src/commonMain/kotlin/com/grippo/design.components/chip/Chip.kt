package com.grippo.design.components.chip

import androidx.compose.foundation.background
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
}

@Composable
public fun Chip(
    modifier: Modifier = Modifier,
    label: String,
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
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(modifier = Modifier.size(AppTokens.dp.chip.trailingSize)) {
            when (trailing) {
                is Trailing.Content -> {
                    trailing.lambda.invoke()
                }

                is Trailing.Icon -> {
                    Icon(
                        modifier = Modifier.size(AppTokens.dp.chip.trailingSize),
                        imageVector = trailing.icon,
                        contentDescription = null,
                        tint = contentColor
                    )
                }
            }
        }


        Spacer(modifier = Modifier.width(AppTokens.dp.chip.spaceBetween))

        Text(
            text = label,
            style = AppTokens.typography.b14Semi(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor
        )

        Spacer(modifier = Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
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
            label = "Basic",
            value = "Value",
            trailing = Trailing.Icon(AppTokens.icons.Weight),
            brush = Brush.linearGradient(listOf(Color.Gray, Color.LightGray)),
            contentColor = Color.Black
        )
    }
}
