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
    accent: Color = AppTokens.colors.selection.accent,
    background: Color = AppTokens.colors.selection.background,
    content: @Composable () -> Unit,
) {
    val shape: Shape = RoundedCornerShape(AppTokens.dp.focusFrame.radius)

    val contentPadding = PaddingValues(
        horizontal = AppTokens.dp.focusFrame.horizontalPadding,
        vertical = AppTokens.dp.focusFrame.verticalPadding,
    )

    Box(
        modifier = modifier
            .background(color = background, shape = shape)
            .border(
                width = AppTokens.dp.focusFrame.borderWidth,
                color = accent,
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
                text = "Highlighted content",
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary,
            )
        }

        FocusFrame(
            modifier = Modifier.fillMaxWidth(),
            accent = AppTokens.colors.semantic.info,
            background = AppTokens.colors.semantic.infoBackground,
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
            background = AppTokens.colors.semantic.successBackground,
        ) {
            Text(
                text = "Success accent",
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary,
            )
        }

        FocusFrame(
            modifier = Modifier.fillMaxWidth(),
            accent = AppTokens.colors.border.default,
            background = AppTokens.colors.background.card,
        ) {
            Text(
                text = "Neutral content",
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary,
            )
        }
    }
}
