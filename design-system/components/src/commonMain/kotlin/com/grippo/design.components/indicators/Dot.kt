package com.grippo.design.components.indicators

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun Dot(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Box(
        modifier = modifier
            .size(AppTokens.dp.dot.size)
            .background(color = color, shape = CircleShape),
    )
}

@AppPreview
@Composable
private fun DotPreview() {
    PreviewContainer {
        listOf(
            "On track" to AppTokens.colors.semantic.success,
            "Drifting" to AppTokens.colors.semantic.warning,
            "Off track" to AppTokens.colors.semantic.error,
            "Not evaluated" to AppTokens.colors.text.secondary,
        ).forEach { (label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Dot(color = color)
                Text(
                    text = label,
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }
    }
}
