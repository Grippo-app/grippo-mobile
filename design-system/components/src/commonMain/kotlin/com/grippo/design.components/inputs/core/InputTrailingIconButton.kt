package com.grippo.design.components.inputs.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens

@Composable
internal fun InputTrailingIconButton(
    modifier: Modifier = Modifier,
    visible: Boolean,
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        modifier = modifier.size(AppTokens.dp.input.trailingActionSize),
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scalableClick(enabled = visible, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.input.icon),
                imageVector = icon,
                tint = tint,
                contentDescription = if (visible) contentDescription else null
            )
        }
    }
}
