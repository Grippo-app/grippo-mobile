package com.grippo.design.components.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun ScreenPlaceholder(
    modifier: Modifier = Modifier,
    text: String
) {
    Column(
        modifier = modifier
            .alpha(0.2f)
            .wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.primary
        )
    }
}

@AppPreview
@Composable
private fun ScreenPlaceholderEmpty() {
    PreviewContainer {
        ScreenPlaceholder(
            text = "Nothing to show"
        )
    }
}