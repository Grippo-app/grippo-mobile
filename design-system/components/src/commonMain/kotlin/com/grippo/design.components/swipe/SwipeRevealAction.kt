package com.grippo.design.components.swipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.delete_btn
import com.grippo.design.resources.provider.icons.Trash

/** An icon action panel matching the height supplied by SwipeToReveal. */
@Composable
public fun SwipeRevealAction(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color = AppTokens.colors.semantic.error,
    background: Color = AppTokens.colors.semantic.errorBackground,
) {
    Box(
        modifier = modifier
            .width(AppTokens.dp.swipe.actionWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(AppTokens.dp.swipe.actionRadius))
            .background(background)
            .scalableClick(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.swipe.actionIconSize),
            imageVector = icon,
            contentDescription = null,
            tint = color,
        )
    }
}

@AppPreview
@Composable
private fun SwipeRevealActionPreview() {
    PreviewContainer {
        SwipeRevealAction(
            modifier = Modifier.height(AppTokens.dp.swipe.actionHeight),
            label = AppTokens.strings.res(Res.string.delete_btn),
            icon = AppTokens.icons.Trash,
            onClick = {},
        )
    }
}
