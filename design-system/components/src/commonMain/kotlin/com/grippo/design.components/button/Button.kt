package com.grippo.design.components.button

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.icons.Loader

@Immutable
public enum class ButtonSize { Small, Medium, Large }

@Immutable
public enum class ButtonStyle { Primary, Secondary, Transparent }

@Immutable
public enum class ButtonState { Enabled, Loading, Disabled; }

@Composable
public fun Button(
    modifier: Modifier = Modifier,
    text: String,
    style: ButtonStyle = ButtonStyle.Primary,
    state: ButtonState = ButtonState.Enabled,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    textStyle: TextStyle = AppTokens.typography.b14Bold(),
) {
    val colorTokens = resolveButtonColors(
        style = style,
        state = state,
    )

    val shape = RoundedCornerShape(AppTokens.dp.shape.medium)

    val height = when (style) {
        ButtonStyle.Transparent -> Dp.Unspecified
        else -> AppTokens.dp.size.componentHeight
    }

    val horizontalPadding = when (style) {
        ButtonStyle.Transparent -> 4.dp
        else -> AppTokens.dp.paddings.mediumHorizontal
    }

    val interactionSource = remember { MutableInteractionSource() }

    val indication = when (style) {
        ButtonStyle.Primary -> LocalIndication.current
        ButtonStyle.Secondary -> LocalIndication.current
        ButtonStyle.Transparent -> null
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colorTokens.background)
            .border(1.dp, colorTokens.border, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = state == ButtonState.Enabled,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding)
            .height(height = height),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state == ButtonState.Loading) {
                val angle = rememberInfiniteTransition().animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    )
                )
                Icon(
                    modifier = Modifier
                        .size(AppTokens.dp.icon.component)
                        .graphicsLayer { rotationZ = angle.value },
                    imageVector = AppTokens.icons.Loader,
                    tint = colorTokens.icon,
                    contentDescription = null,
                )
            } else {
                if (icon != null) {
                    Icon(
                        modifier = Modifier.size(AppTokens.dp.icon.component),
                        imageVector = icon,
                        tint = colorTokens.icon,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = colorTokens.content,
                    style = textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
