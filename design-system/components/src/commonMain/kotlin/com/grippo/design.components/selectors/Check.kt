package com.grippo.design.components.selectors

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Check

/**
 * Shared check control. With no [onCheckedChange], draws only the indicator;
 * the enclosing selectable surface owns interaction and accessibility.
 */
@Composable
public fun Check(
    modifier: Modifier = Modifier,
    checked: Boolean,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onCheckedChange: (() -> Unit)? = null,
) {
    val dp = AppTokens.dp.check
    val colors = AppTokens.colors.selection
    val accent = if (enabled) colors.accent else AppTokens.colors.icon.disabled
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) accent else Color.Transparent,
        label = "CheckBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppTokens.colors.icon.disabled
            checked -> accent
            else -> AppTokens.colors.icon.secondary
        },
        label = "CheckBorder",
    )
    val markColor by animateColorAsState(
        targetValue = if (checked) colors.onAccent else Color.Transparent,
        label = "CheckMark",
    )
    val interactionModifier = if (onCheckedChange != null) {
        Modifier
            .semantics {
                role = Role.Checkbox
                if (contentDescription != null) this.contentDescription = contentDescription
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                if (!enabled) disabled()
            }
            .scalableClick(
                enabled = enabled,
                haptic = true,
                role = Role.Checkbox,
                onClick = onCheckedChange,
            )
            .sizeIn(minWidth = dp.touchSize, minHeight = dp.touchSize)
    } else {
        Modifier
    }
    Box(
        modifier = modifier.then(interactionModifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(dp.size)
                .background(backgroundColor, CircleShape)
                .border(
                    width = dp.borderWidth,
                    color = borderColor,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(dp.iconSize),
                imageVector = AppTokens.icons.Check,
                contentDescription = null,
                tint = markColor,
            )
        }
    }
}

@AppPreview
@Composable
private fun CheckIndicatorPreview() {
    PreviewContainer {
        Check(checked = false)
        Check(checked = true)
        Check(checked = false, enabled = false)
        Check(checked = true, enabled = false)
    }
}

@AppPreview
@Composable
private fun CheckPreview() {
    PreviewContainer {
        Check(checked = false, onCheckedChange = {})
        Check(checked = true, onCheckedChange = {})
        Check(checked = false, enabled = false, onCheckedChange = {})
        Check(checked = true, enabled = false, onCheckedChange = {})
    }
}
