package com.grippo.design.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.grippo.design.core.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview(
    name = "Phone • UK • Small",
    group = "📱 Phone",
    widthDp = 360, heightDp = 800,
    locale = "uk"
)
@Preview(
    name = "Phone • EN • Big",
    group = "📱 Phone",
    widthDp = 412, heightDp = 915,
    locale = "en"
)
public annotation class AppPreview

@OptIn(ExperimentalCoilApi::class)
@Composable
public fun PreviewContainer(
    content: @Composable ColumnScope.() -> Unit,
) {
    val previewHandler = AsyncImagePreviewHandler {
        ColorImage(Color.Black.copy(alpha = 0.2f).toArgb())
    }

    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        Column {
            AppTheme(darkTheme = true, localeTag = "en") {
                Column(
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(12.dp),
                    content = content,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                )
            }
        }
    }
}