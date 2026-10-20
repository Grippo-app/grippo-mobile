package com.grippo.design.components.datetime

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun DialogRangeHeader(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppTokens.dp.dialog.horizontalPadding,
                end = AppTokens.dp.dialog.horizontalPadding,
                bottom = AppTokens.dp.contentPadding.content,
            ),
        text = text,
        style = AppTokens.typography.b14Med(),
        color = AppTokens.colors.text.secondary,
        textAlign = TextAlign.Center,
    )
}

@AppPreview
@Composable
private fun ShortRangePreview() {
    PreviewContainer {
        DialogRangeHeader(text = "11 Oct - 17 Oct")
    }
}

@AppPreview
@Composable
private fun LongRangePreview() {
    PreviewContainer {
        DialogRangeHeader(text = "30 вер. 2025 - 20 жовт. 2026")
    }
}
