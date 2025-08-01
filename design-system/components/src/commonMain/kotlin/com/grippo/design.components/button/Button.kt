package com.grippo.design.components.button

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.SystemRestart

@Immutable
public enum class ButtonSize { Small, Medium, Large }

@Immutable
public sealed interface ButtonStyle {
    public data object Primary : ButtonStyle
    public data object Secondary : ButtonStyle
    public data object Transparent : ButtonStyle
    public data class Custom(
        val enabled: ButtonColorTokens,
        val disabled: ButtonColorTokens,
    ) : ButtonStyle
}

@Immutable
public enum class ButtonState { Enabled, Loading, Disabled; }

@Composable
public fun Button(
    modifier: Modifier = Modifier,
    text: String,
    style: ButtonStyle = ButtonStyle.Primary,
    state: ButtonState = ButtonState.Enabled,
    onClick: () -> Unit,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    textStyle: TextStyle = AppTokens.typography.b14Bold(),
) {
    val colorTokens = resolveButtonColors(
        style = style,
        state = state,
    )

    val shape = RoundedCornerShape(AppTokens.dp.button.radius)

    val height = when (style) {
        ButtonStyle.Transparent -> null
        else -> AppTokens.dp.button.height
    }

    val horizontalPadding = when (style) {
        ButtonStyle.Transparent -> null
        else -> AppTokens.dp.button.horizontalPadding
    }

    val iconPadding = when (style) {
        ButtonStyle.Transparent -> AppTokens.dp.button.spaceTransparent
        else -> AppTokens.dp.button.space
    }

    val baseModifier = modifier
        .scalableClick(
            enabled = state == ButtonState.Enabled,
            onClick = onClick
        )
        .background(colorTokens.background, shape)
        .border(1.dp, colorTokens.border, shape)

    val paddedModifier = horizontalPadding?.let {
        baseModifier.padding(horizontal = it)
    } ?: baseModifier

    val finalModifier = height?.let {
        paddedModifier.height(it)
    } ?: paddedModifier

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.animateContentSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val iconSize = AppTokens.dp.button.icon

            when (state) {
                ButtonState.Loading -> {
                    val angle by rememberInfiniteTransition().animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        )
                    )

                    Icon(
                        modifier = Modifier
                            .size(iconSize)
                            .rotate(angle),
                        imageVector = AppTokens.icons.SystemRestart,
                        tint = colorTokens.icon,
                        contentDescription = null,
                    )

                    Spacer(modifier = Modifier.width(iconPadding))
                }

                else -> {
                    if (startIcon != null) {
                        Icon(
                            modifier = Modifier.size(iconSize),
                            imageVector = startIcon,
                            tint = colorTokens.icon,
                            contentDescription = null,
                        )

                        Spacer(modifier = Modifier.width(iconPadding))
                    }
                }
            }

            Text(
                text = text,
                color = colorTokens.content,
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (endIcon != null) {
                Spacer(modifier = Modifier.width(iconPadding))

                Icon(
                    modifier = Modifier.size(iconSize),
                    imageVector = endIcon,
                    tint = colorTokens.icon,
                    contentDescription = null,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ButtonPrimaryPreviews() {
    PreviewContainer {
        ButtonVariantsWithIcon(ButtonStyle.Primary)
    }
}

@AppPreview
@Composable
private fun ButtonSecondaryPreviews() {
    PreviewContainer {
        ButtonVariantsWithIcon(ButtonStyle.Secondary)
    }
}

@AppPreview
@Composable
private fun ButtonTransparentPreviews() {
    PreviewContainer {
        ButtonVariantsWithIcon(ButtonStyle.Transparent)
    }
}

@Composable
private fun ButtonVariantsWithIcon(style: ButtonStyle) {
    Button(
        text = "Enabled",
        style = style,
        state = ButtonState.Enabled,
        onClick = {},
        startIcon = Icons.Default.Check
    )
    Button(
        text = "Loading",
        style = style,
        state = ButtonState.Loading,
        onClick = {},
        startIcon = Icons.Default.Check
    )
    Button(
        text = "Disabled",
        style = style,
        state = ButtonState.Disabled,
        onClick = {},
        startIcon = Icons.Default.Check
    )
}