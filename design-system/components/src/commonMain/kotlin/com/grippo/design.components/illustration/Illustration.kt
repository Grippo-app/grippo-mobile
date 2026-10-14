package com.grippo.design.components.illustration

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.error_dialog

@Composable
public fun Illustration(
    modifier: Modifier = Modifier,
    painter: Painter,
    style: IllustrationStyle = IllustrationStyle.Large,
) {
    val size = when (style) {
        IllustrationStyle.Compact -> AppTokens.dp.illustration.compact
        IllustrationStyle.Medium -> AppTokens.dp.illustration.medium
        IllustrationStyle.Large -> AppTokens.dp.illustration.large
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            modifier = Modifier.widthIn(max = size).fillMaxWidth().aspectRatio(1f),
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
    }
}

@AppPreview
@Composable
private fun IllustrationPreview() {
    PreviewContainer {
        Illustration(
            modifier = Modifier.fillMaxWidth(),
            painter = AppTokens.drawables.res(Res.drawable.error_dialog),
            style = IllustrationStyle.Compact,
        )
    }
}
