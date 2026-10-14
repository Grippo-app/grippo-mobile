package com.grippo.design.components.selectors

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

@Composable
public fun Check(
    modifier: Modifier = Modifier,
    checked: Boolean,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onCheckedChange: () -> Unit,
) {
    val dp = AppTokens.dp.check
    val shape = RoundedCornerShape(dp.radius)
    val click by rememberUpdatedState(onCheckedChange)
    val markAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "CheckMark",
    )
    val color = if (enabled) AppTokens.colors.icon.secondary else AppTokens.colors.icon.disabled
    Box(
        modifier = modifier
            .semantics {
                role = Role.Checkbox
                if (contentDescription != null) this.contentDescription = contentDescription
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                if (!enabled) disabled()
            }
            .scalableClick(enabled = enabled, haptic = true, role = Role.Checkbox, onClick = click)
            .sizeIn(minWidth = dp.touchSize, minHeight = dp.touchSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(dp.size)
                .background(if (checked) color else Color.Transparent, shape)
                .border(
                    width = dp.borderWidth,
                    color = color,
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(dp.iconSize).graphicsLayer { alpha = markAlpha },
                imageVector = AppTokens.icons.Check,
                contentDescription = null,
                tint = AppTokens.colors.background.screen,
            )
        }
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
