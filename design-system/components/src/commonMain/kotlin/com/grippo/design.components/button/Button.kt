package com.grippo.design.components.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.button.internal.resolveButtonColors
import com.grippo.design.components.button.internal.resolveButtonSize
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.modifiers.shimmer
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Google

@Immutable
public sealed interface ButtonContent {
    @Immutable
    public data class Text(
        val text: String,
        val startIcon: ButtonIcon? = null,
        val endIcon: ButtonIcon? = null,
    ) : ButtonContent

    @Immutable
    public data class Icon(
        val icon: ButtonIcon,
    ) : ButtonContent
}

@Immutable
public sealed class ButtonIcon(
    public open val value: ImageVector
) {
    @Immutable
    public data class Icon(
        public override val value: ImageVector
    ) : ButtonIcon(value)

    @Immutable
    public data class Image(
        public override val value: ImageVector
    ) : ButtonIcon(value)
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
}

@Immutable
public enum class ButtonState {
    Enabled,
    Loading,
    Disabled
}

/** Loading preserves the label, icons and measured width while a shimmer crosses the surface. */
@Composable
public fun Button(
    modifier: Modifier = Modifier,
    content: ButtonContent,
    style: ButtonStyle = ButtonStyle.Primary,
    state: ButtonState = ButtonState.Enabled,
    size: ButtonSize = ButtonSize.Medium,
    onClick: () -> Unit,
    textStyle: TextStyle = when (size) {
        ButtonSize.Small -> AppTokens.typography.b12Semi()
        ButtonSize.Medium -> AppTokens.typography.b14Bold()
    },
) {
    val colors = resolveButtonColors(style, state)
    val metrics = resolveButtonSize(size)
    val transparent = style == ButtonStyle.Transparent
    val loading = state == ButtonState.Loading
    val enabled = state == ButtonState.Enabled
    val tintImages = state == ButtonState.Disabled
    val highlight = when (style) {
        ButtonStyle.Primary -> AppTokens.colors.shimmer.buttonPrimary
        ButtonStyle.Secondary -> AppTokens.colors.shimmer.buttonSecondary
        ButtonStyle.Tertiary -> AppTokens.colors.shimmer.buttonTertiary
        ButtonStyle.Transparent -> AppTokens.colors.shimmer.buttonTransparent
        ButtonStyle.Error -> AppTokens.colors.shimmer.buttonError
    }
    val gap = if (transparent) metrics.spaceTransparent else metrics.space
    val baseModifier = modifier
        .semantics(mergeDescendants = true) {
            role = Role.Button
            if (!enabled) disabled()
            if (loading) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }
        .scalableClick(
            enabled = enabled,
            onClick = onClick,
            haptic = style == ButtonStyle.Primary || style == ButtonStyle.Secondary ||
                style == ButtonStyle.Error,
        )
        .clip(CircleShape)
        .background(Brush.horizontalGradient(listOf(colors.background1, colors.background2)))
        .border(AppTokens.dp.button.border, colors.border, CircleShape)
        .shimmer(enabled = loading, highlightColor = highlight)

    when (content) {
        is ButtonContent.Text -> Row(
            modifier = baseModifier.then(
                if (transparent) Modifier else Modifier
                    .heightIn(min = metrics.height)
                    .padding(
                        horizontal = metrics.horizontalPadding,
                        vertical = AppTokens.dp.button.verticalPadding,
                    )
            ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content.startIcon?.let { icon ->
                ButtonIconContent(Modifier.size(metrics.icon), icon, colors.icon, tintImages)
                Spacer(Modifier.width(gap))
            }
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = content.text,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                color = colors.content,
                style = textStyle,
            )
            content.endIcon?.let { icon ->
                Spacer(Modifier.width(gap))
                ButtonIconContent(Modifier.size(metrics.icon), icon, colors.icon, tintImages)
            }
        }
        is ButtonContent.Icon -> Box(
            modifier = baseModifier.size(metrics.height),
            contentAlignment = Alignment.Center,
        ) {
            ButtonIconContent(Modifier.size(metrics.icon), content.icon, colors.icon, tintImages)
        }
    }
}

@Composable
private fun ButtonIconContent(
    modifier: Modifier = Modifier,
    icon: ButtonIcon,
    tint: Color,
    tintImageIcons: Boolean,
) {
    when (icon) {
        is ButtonIcon.Icon -> {
            Icon(
                modifier = modifier,
                imageVector = icon.value,
                tint = tint,
                contentDescription = null
            )
        }

        is ButtonIcon.Image -> {
            Image(
                modifier = modifier,
                imageVector = icon.value,
                colorFilter = if (tintImageIcons) ColorFilter.tint(tint) else null,
                contentDescription = null
            )
        }
    }
}

@AppPreview
@Composable
private fun ButtonImagePreview() {
    PreviewContainer {
        Button(
            content = ButtonContent.Text(
                text = "Enabled",
                startIcon = ButtonIcon.Image(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Primary,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Text(
                text = "Enabled",
                startIcon = ButtonIcon.Image(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Primary,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(
                icon = ButtonIcon.Image(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Secondary,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(
                icon = ButtonIcon.Image(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Secondary,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )
    }
}

@AppPreview
@Composable
private fun ButtonPrimaryPreview() {
    PreviewContainer {
        Button(
            content = ButtonContent.Text(
                text = "Enabled",
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Primary,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Loading",
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Primary,
            state = ButtonState.Loading,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Disabled",
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Primary,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
            style = ButtonStyle.Primary,
            state = ButtonState.Enabled,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
            style = ButtonStyle.Primary,
            state = ButtonState.Loading,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
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
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Secondary,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Loading",
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Secondary,
            state = ButtonState.Loading,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Disabled",
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Secondary,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
            style = ButtonStyle.Secondary,
            state = ButtonState.Enabled,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
            style = ButtonStyle.Secondary,
            state = ButtonState.Loading,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
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
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Transparent,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Loading",
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Transparent,
            state = ButtonState.Loading,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Disabled",
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Transparent,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
            style = ButtonStyle.Transparent,
            state = ButtonState.Enabled,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
            style = ButtonStyle.Transparent,
            state = ButtonState.Loading,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
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
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Tertiary,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Loading",
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Tertiary,
            state = ButtonState.Loading,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Disabled",
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Tertiary,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
            style = ButtonStyle.Tertiary,
            state = ButtonState.Enabled,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
            style = ButtonStyle.Tertiary,
            state = ButtonState.Loading,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
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
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Error,
            state = ButtonState.Enabled,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Loading",
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Error,
            state = ButtonState.Loading,
            size = ButtonSize.Medium,
            onClick = {}
        )
        Button(
            content = ButtonContent.Text(
                text = "Disabled",
                startIcon = ButtonIcon.Icon(AppTokens.icons.Google)
            ),
            style = ButtonStyle.Error,
            state = ButtonState.Disabled,
            size = ButtonSize.Medium,
            onClick = {}
        )

        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
            style = ButtonStyle.Error,
            state = ButtonState.Enabled,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
            style = ButtonStyle.Error,
            state = ButtonState.Loading,
            size = ButtonSize.Small,
            onClick = {}
        )
        Button(
            content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.Google)),
            style = ButtonStyle.Error,
            state = ButtonState.Disabled,
            size = ButtonSize.Small,
            onClick = {}
        )
    }
}
