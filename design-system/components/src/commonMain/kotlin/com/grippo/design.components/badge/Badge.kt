package com.grippo.design.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.formatters.UiText
import com.grippo.design.components.badge.internal.resolveBadgeTokens
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Stable
public sealed interface BadgeStyle {

    @Stable
    public data object Error : BadgeStyle

    @Stable
    public data object Warning : BadgeStyle

    @Stable
    public data object Success : BadgeStyle
}

@Composable
public fun Badge(
    modifier: Modifier = Modifier,
    label: UiText? = null,
    style: BadgeStyle,
    onClick: (() -> Unit)? = null,
) {
    val tokens = resolveBadgeTokens(style)
    val dp = AppTokens.dp.badge

    val verticalPadding = when {
        label == null -> dp.horizontalPadding
        else -> dp.verticalPadding
    }
    Row(
        modifier = modifier
            .background(color = tokens.background, shape = CircleShape)
            .let { if (onClick != null) it.scalableClick(onClick = onClick) else it }
            .padding(horizontal = dp.horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dp.spaceBetween),
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
                color = tokens.content,
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
        // Error — with label and icon-only
        Badge(label = UiText.Str(value = "Error"), style = BadgeStyle.Error)
        Badge(style = BadgeStyle.Error)

        // Warning — with label and icon-only
        Badge(label = UiText.Str(value = "Warning"), style = BadgeStyle.Warning)
        Badge(style = BadgeStyle.Warning)

        // Success — with label and icon-only
        Badge(label = UiText.Str(value = "Success"), style = BadgeStyle.Success)
        Badge(style = BadgeStyle.Success)

        // Clickable
        Badge(label = UiText.Str(value = "Clickable"), style = BadgeStyle.Success, onClick = {})
    }
}
