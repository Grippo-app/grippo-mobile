package com.grippo.design.components.frames

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun FocusFrame(
    modifier: Modifier = Modifier,
    accent: Color = AppTokens.colors.semantic.notice,
    content: @Composable () -> Unit,
) {
    val shape: Shape = RoundedCornerShape(AppTokens.dp.focusFrame.radius)

    val contentPadding = PaddingValues(
        horizontal = AppTokens.dp.focusFrame.horizontalPadding,
        vertical = AppTokens.dp.focusFrame.verticalPadding,
    )

    Box(
        modifier = modifier
            .background(color = accent.copy(alpha = 0.12f), shape = shape)
            .border(
                width = AppTokens.dp.focusFrame.borderWidth,
                color = accent.copy(alpha = 0.28f),
                shape = shape
            )
            .padding(contentPadding),
    ) {
        content()
    }
}

@AppPreview
@Composable
private fun FocusFramePreview() {
    PreviewContainer {
        FocusFrame(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Default — semantic.notice",
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary,
            )
        }

        FocusFrame(
            modifier = Modifier.fillMaxWidth(),
            accent = AppTokens.colors.semantic.info,
        ) {
            Text(
                text = "Info accent",
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary,
            )
        }

        FocusFrame(
            modifier = Modifier.fillMaxWidth(),
            accent = AppTokens.colors.semantic.success,
        ) {
            Text(
                text = "Success accent",
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary,
            )
        }

        FocusFrame(
            modifier = Modifier.fillMaxWidth(),
            accent = AppTokens.colors.brand.color2,
        ) {
            Text(
                text = "Custom brand accent",
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary,
            )
        }
    }
}
