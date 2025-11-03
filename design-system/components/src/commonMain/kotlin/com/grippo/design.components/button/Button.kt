package com.grippo.design.components.button

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
public sealed interface ButtonSize {
    @Immutable
    public data object Small : ButtonSize

    @Immutable
    public data object Medium : ButtonSize
}

@Immutable
public sealed interface ButtonStyle {
    // Basic
    public data object Primary : ButtonStyle
    public data object Secondary : ButtonStyle
    public data object Tertiary : ButtonStyle
    public data object Transparent : ButtonStyle

    // Specific
    public data object Error : ButtonStyle
    public data object Magic : ButtonStyle
}

@Immutable
public enum class ButtonState {
    Enabled,
    Loading,
    Disabled
}

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
    size: ButtonSize = ButtonSize.Medium,
    onClick: () -> Unit,
    textStyle: TextStyle = AppTokens.typography.b14Bold(),
) {
    val colorTokens = resolveButtonColors(
        style = style,
        state = state
    )

    val metrics = resolveButtonSize(
        size = size
    )

    val shape = CircleShape

    val iconSize = metrics.icon
    val isLoading = state == ButtonState.Loading

    // Text layout metrics (Transparent keeps flexible height/paddings)
    val textHeight = if (style == ButtonStyle.Transparent) null else metrics.height
    val horizontalPadding =
        if (style == ButtonStyle.Transparent) null else metrics.horizontalPadding
    val iconPadding =
        if (style == ButtonStyle.Transparent) metrics.spaceTransparent else metrics.space

    // Minimum side for square icon-only layout
    val minSide = metrics.height

    val baseModifier = modifier
        .scalableClick(
            enabled = state == ButtonState.Enabled,
            onClick = onClick
        ).background(
            Brush.horizontalGradient(
                0f to colorTokens.background1,
                1f to colorTokens.background2,
            ), shape
        ).border(
            2.dp,
            colorTokens.border,
            shape
        )

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
                // Start slot
                if (content.startIcon != null) {
                    // Crossfade loader <-> start icon (width preserved)
                    StartIconOrLoader(
                        isLoading = isLoading,
                        icon = content.startIcon,
                        size = iconSize,
                        tint = colorTokens.icon,
                        label = "btn_text_start_crossfade"
                    )
                    Spacer(modifier = Modifier.width(iconPadding))
                } else {
                    // Two-phase removal: fade out first, then collapse width to 0
                    CollapsingLoaderSlot(
                        visible = isLoading,
                        size = iconSize,
                        spaceAfter = iconPadding,
                        tint = colorTokens.icon,
                        label = "btn_text_loader_collapsing"
                    )
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
                    Icon(
                        modifier = Modifier.size(iconSize),
                        imageVector = content.endIcon,
                        tint = colorTokens.icon,
                        contentDescription = null
                    )
                }
            }
        }

        is ButtonContent.Icon -> {
            // Strict square icon-only button
            Box(
                modifier = baseModifier.size(minSide),
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

@Composable
private fun StartIconOrLoader(
    isLoading: Boolean,
    icon: ImageVector,
    size: Dp,
    tint: Color,
    label: String
) {
    // Separate transition tracks so loader fades out before/after as needed
    val transition = updateTransition(targetState = isLoading, label = label)

    val loaderAlpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) { // appear
                tween(durationMillis = 160, delayMillis = 120, easing = LinearEasing)
            } else { // disappear
                tween(durationMillis = 160, easing = LinearEasing)
            }
        },
        label = "$label-loaderAlpha"
    ) { if (it) 1f else 0f }

    val iconAlpha by transition.animateFloat(
        transitionSpec = {
            if (true isTransitioningTo false) { // appear
                tween(durationMillis = 160, delayMillis = 120, easing = LinearEasing)
            } else { // disappear
                tween(durationMillis = 160, easing = LinearEasing)
            }
        },
        label = "$label-iconAlpha"
    ) { if (it) 0f else 1f }

    val rot = rememberInfiniteTransition(label = "$label-rot")
    val angle by rot.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "$label-rotation"
    )

    // Stack loader and icon; width is constant (the Row's spacer handles gap)
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        // Icon layer
        Icon(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = iconAlpha },
            imageVector = icon,
            tint = tint,
            contentDescription = null
        )
        // Loader layer
        Icon(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = loaderAlpha
                    rotationZ = angle
                },
            imageVector = AppTokens.icons.SystemRestart,
            tint = tint,
            contentDescription = null
        )
    }
}

@Composable
private fun CollapsingLoaderSlot(
    visible: Boolean,
    size: Dp,
    spaceAfter: Dp,
    tint: Color,
    label: String
) {
    // Two tracks: alpha fades immediately; width collapses after alpha completes
    val transition = updateTransition(targetState = visible, label = label)

    val alpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                tween(durationMillis = 160, delayMillis = 80, easing = LinearEasing)
            } else {
                tween(durationMillis = 160, easing = LinearEasing)
            }
        },
        label = "$label-alpha"
    ) { if (it) 1f else 0f }

    val width by transition.animateDp(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Expanding: width first, then fade-in starts (handled by alpha delay above)
                tween(durationMillis = 180, easing = LinearOutSlowInEasing)
            } else {
                // Collapsing: start after fade-out finished
                tween(durationMillis = 220, delayMillis = 160, easing = LinearOutSlowInEasing)
            }
        },
        label = "$label-width"
    ) { if (it) size + spaceAfter else 0.dp }

    if (width > 0.dp) {
        val rot = rememberInfiniteTransition(label = "$label-rot")
        val angle by rot.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "$label-rotation"
        )

        Row(
            modifier = Modifier.width(width),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        this.alpha = alpha
                        rotationZ = angle
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppTokens.icons.SystemRestart,
                    tint = tint,
                    contentDescription = null
                )
            }
            // Trailing space is baked into `width` via `size + spaceAfter`
        }
    }
}

@AppPreview
@Composable
private fun ButtonPrimaryPreview() {
    PreviewContainer {
        Button(
            content = ButtonContent.Text(
                text = "Enabled",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Primary,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Loading",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Primary,
            state = ButtonState.Loading,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Disabled",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Primary,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Primary,
            state = ButtonState.Enabled,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Primary,
            state = ButtonState.Loading,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Primary,
            state = ButtonState.Disabled,
            size = ButtonSize.Small,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ButtonSecondaryPreview() {
    PreviewContainer {
        Button(
            content = ButtonContent.Text(
                text = "Enabled",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Secondary,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Loading",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Secondary,
            state = ButtonState.Loading,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Disabled",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Secondary,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Secondary,
            state = ButtonState.Enabled,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Secondary,
            state = ButtonState.Loading,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Secondary,
            state = ButtonState.Disabled,
            size = ButtonSize.Small,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ButtonTransparentPreview() {
    PreviewContainer {
        Button(
            content = ButtonContent.Text(
                text = "Enabled",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Transparent,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Loading",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Transparent,
            state = ButtonState.Loading,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Disabled",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Transparent,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Transparent,
            state = ButtonState.Enabled,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Transparent,
            state = ButtonState.Loading,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Transparent,
            state = ButtonState.Disabled,
            size = ButtonSize.Small,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ButtonTertiaryPreview() {
    PreviewContainer {
        Button(
            content = ButtonContent.Text(
                text = "Enabled",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Tertiary,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Loading",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Tertiary,
            state = ButtonState.Loading,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Disabled",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Tertiary,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Tertiary,
            state = ButtonState.Enabled,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Tertiary,
            state = ButtonState.Loading,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Tertiary,
            state = ButtonState.Disabled,
            size = ButtonSize.Small,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ButtonErrorPreview() {
    PreviewContainer {
        Button(
            content = ButtonContent.Text(
                text = "Enabled",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Error,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Loading",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Error,
            state = ButtonState.Loading,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Disabled",
                startIcon = AppTokens.icons.SystemRestart
            ),
            style = ButtonStyle.Error,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Error,
            state = ButtonState.Enabled,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Error,
            state = ButtonState.Loading,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = AppTokens.icons.SystemRestart),
            style = ButtonStyle.Error,
            state = ButtonState.Disabled,
            size = ButtonSize.Small,
            onClick = {}
        )
    }
}