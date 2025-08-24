package com.grippo.design.components.button

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.SystemRestart

@Immutable
public sealed interface ButtonContent {
    @Immutable
    public data class Text(
        val text: String,
        val startIcon: ImageVector? = null,
        val endIcon: ImageVector? = null,
    ) : ButtonContent

    @Immutable
    public data class Icon(
        val icon: ImageVector,
    ) : ButtonContent
}

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
public enum class ButtonState { Enabled, Loading, Disabled }

/**
 * Canonical button with two layouts:
 * - ButtonContent.Text: standard (text with optional start/end icons)
 * - ButtonContent.Icon: square icon-only button
 */
@Composable
public fun Button(
    modifier: Modifier = Modifier,
    content: ButtonContent,
    style: ButtonStyle = ButtonStyle.Primary,
    state: ButtonState = ButtonState.Enabled,
    onClick: () -> Unit,
    textStyle: TextStyle = AppTokens.typography.b14Bold(),
) {
    val colorTokens = resolveButtonColors(style = style, state = state)
    val shape = RoundedCornerShape(AppTokens.dp.button.radius)
    val iconSize = AppTokens.dp.button.icon
    val isLoading = state == ButtonState.Loading

    // Size & paddings for text layout (keeps Transparent behavior)
    val textHeight = if (style == ButtonStyle.Transparent) null else AppTokens.dp.button.height
    val horizontalPadding =
        if (style == ButtonStyle.Transparent) null else AppTokens.dp.button.horizontalPadding
    val iconPadding =
        if (style == ButtonStyle.Transparent) AppTokens.dp.button.spaceTransparent else AppTokens.dp.button.space

    // Square side for icon-only layout (aligned with button height token)
    val squareSide = AppTokens.dp.button.height

    val baseModifier = modifier
        .scalableClick(enabled = state == ButtonState.Enabled, onClick = onClick)
        .background(colorTokens.background, shape)
        .border(1.dp, colorTokens.border, shape)

    val loadingTransition = rememberInfiniteTransition(label = "button_loading")

    when (content) {
        is ButtonContent.Text -> {
            val finalModifier = baseModifier
                .then(horizontalPadding?.let { Modifier.padding(horizontal = it) } ?: Modifier)
                .then(textHeight?.let { Modifier.height(it) } ?: Modifier)

            Row(
                modifier = finalModifier,
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start icon or loader
                AnimatedContent(
                    targetState = isLoading,
                    transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() },
                    label = "btn_text_start_anim"
                ) { loading ->
                    if (loading) {
                        val angle by loadingTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "btn_text_loader_rotation"
                        )
                        Icon(
                            modifier = Modifier
                                .size(iconSize)
                                .graphicsLayer { rotationZ = angle },
                            imageVector = AppTokens.icons.SystemRestart,
                            tint = colorTokens.icon,
                            contentDescription = null
                        )
                    } else {
                        content.startIcon?.let {
                            Icon(
                                modifier = Modifier.size(iconSize),
                                imageVector = it,
                                tint = colorTokens.icon,
                                contentDescription = null
                            )
                        }
                    }
                }

                if (content.startIcon != null || isLoading) {
                    Spacer(modifier = Modifier.width(iconPadding))
                }

                Text(
                    text = content.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorTokens.content,
                    style = textStyle
                )

                if (content.endIcon != null) {
                    Spacer(modifier = Modifier.width(iconPadding))
                }

                content.endIcon?.let {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        imageVector = it,
                        tint = colorTokens.icon,
                        contentDescription = null
                    )
                }
            }
        }

        is ButtonContent.Icon -> {
            // Strict square icon-only button
            Box(
                modifier = baseModifier.size(squareSide),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isLoading,
                    transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() },
                    label = "btn_icon_anim"
                ) { loading ->
                    if (loading) {
                        val angle by loadingTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "btn_icon_loader_rotation"
                        )
                        Icon(
                            modifier = Modifier
                                .size(iconSize)
                                .graphicsLayer { rotationZ = angle },
                            imageVector = AppTokens.icons.SystemRestart,
                            tint = colorTokens.icon,
                            contentDescription = null
                        )
                    } else {
                        Icon(
                            modifier = Modifier.size(iconSize),
                            imageVector = content.icon,
                            tint = colorTokens.icon,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ButtonTextPreview() {
    PreviewContainer {
        Button(
            content = ButtonContent.Text(text = "Enabled", startIcon = Icons.Default.Check),
            style = ButtonStyle.Primary,
            state = ButtonState.Enabled,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(text = "Loading", startIcon = Icons.Default.Check),
            style = ButtonStyle.Primary,
            state = ButtonState.Loading,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(text = "Disabled", startIcon = Icons.Default.Check),
            style = ButtonStyle.Primary,
            state = ButtonState.Disabled,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ButtonIconPreview() {
    PreviewContainer {
        Button(
            content = ButtonContent.Icon(icon = Icons.Default.Check),
            style = ButtonStyle.Secondary,
            state = ButtonState.Enabled,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = Icons.Default.Check),
            style = ButtonStyle.Secondary,
            state = ButtonState.Loading,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = Icons.Default.Check),
            style = ButtonStyle.Secondary,
            state = ButtonState.Disabled,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ButtonTextTransparentPreview() {
    PreviewContainer {
        Button(
            content = ButtonContent.Text(text = "Enabled", startIcon = Icons.Default.Check),
            style = ButtonStyle.Transparent,
            state = ButtonState.Enabled,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(text = "Loading", startIcon = Icons.Default.Check),
            style = ButtonStyle.Transparent,
            state = ButtonState.Loading,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(text = "Disabled", startIcon = Icons.Default.Check),
            style = ButtonStyle.Transparent,
            state = ButtonState.Disabled,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ButtonIconTransparentPreview() {
    PreviewContainer {
        Button(
            content = ButtonContent.Icon(icon = Icons.Default.Check),
            style = ButtonStyle.Transparent,
            state = ButtonState.Enabled,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = Icons.Default.Check),
            style = ButtonStyle.Transparent,
            state = ButtonState.Loading,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = Icons.Default.Check),
            style = ButtonStyle.Transparent,
            state = ButtonState.Disabled,
            onClick = {}
        )
    }
}