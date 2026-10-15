package com.grippo.design.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.formatters.UiText
import com.grippo.design.components.badge.internal.resolveBadgeTokens
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.accessibility_status_error
import com.grippo.design.resources.provider.accessibility_status_success
import com.grippo.design.resources.provider.accessibility_status_warning

@Immutable
public sealed interface BadgeStyle {

    @Immutable
    public data object Error : BadgeStyle

    @Immutable
    public data object Warning : BadgeStyle

    @Immutable
    public data object Success : BadgeStyle
}

@Composable
public fun Badge(
    modifier: Modifier = Modifier,
    label: UiText? = null,
    style: BadgeStyle,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
) {
    val tokens = resolveBadgeTokens(style)
    val statusLabel = AppTokens.strings.res(
        when (style) {
            BadgeStyle.Error -> Res.string.accessibility_status_error
            BadgeStyle.Warning -> Res.string.accessibility_status_warning
            BadgeStyle.Success -> Res.string.accessibility_status_success
        },
    )

    val dp = AppTokens.dp.badge
    val shape = RoundedCornerShape(dp.radius)
    val horizontalPadding = if (label == null) dp.iconOnlyPadding else dp.horizontalPadding
    val verticalPadding = if (label == null) dp.iconOnlyPadding else dp.verticalPadding

    Row(
        modifier = modifier
            .clip(shape)
            .background(color = tokens.background, shape = shape)
            .semantics(mergeDescendants = true) {
                stateDescription = statusLabel
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
            .let { if (onClick != null) it.scalableClick(role = Role.Button, onClick = onClick) else it }
            .sizeIn(minWidth = dp.minSize, minHeight = dp.minSize)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dp.spaceBetween, Alignment.CenterHorizontally),
    ) {
        Icon(
            modifier = Modifier.size(dp.icon),
            imageVector = tokens.icon,
            contentDescription = null,
            tint = tokens.content,
        )

        if (label != null) {
            Text(
                text = label.text(),
                style = AppTokens.typography.b11Semi(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@AppPreview
@Composable
private fun BadgePreview() {
    PreviewContainer {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Badge(label = UiText.Str(value = "Error"), style = BadgeStyle.Error)
            Badge(style = BadgeStyle.Error)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Badge(label = UiText.Str(value = "Warning"), style = BadgeStyle.Warning)
            Badge(style = BadgeStyle.Warning)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Badge(label = UiText.Str(value = "Success"), style = BadgeStyle.Success)
            Badge(style = BadgeStyle.Success)
        }

        Badge(label = UiText.Str(value = "Clickable"), style = BadgeStyle.Success, onClick = {})
        Badge(
            modifier = Modifier.fillMaxWidth(),
            label = UiText.Str(value = "Довге локалізоване повідомлення про помилку"),
            style = BadgeStyle.Error,
        )
    }
}
