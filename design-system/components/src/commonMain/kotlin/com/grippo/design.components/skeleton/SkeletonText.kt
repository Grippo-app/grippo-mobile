package com.grippo.design.components.skeleton

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun SkeletonText(
    modifier: Modifier = Modifier,
    style: TextStyle,
    textMeasurer: TextMeasurer = rememberTextMeasurer(),
    animated: Boolean = false,
) {
    val height = rememberTextHeight(style = style, textMeasurer = textMeasurer)

    SkeletonBone(modifier = modifier.height(height), animated = animated)
}

@AppPreview
@Composable
private fun SkeletonTextPreview() {
    PreviewContainer {
        SkeletonText(
            modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
            style = AppTokens.typography.h4(),
        )
        SkeletonText(
            modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
            style = AppTokens.typography.b13Med(),
        )
        SkeletonText(
            modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.short),
            style = AppTokens.typography.b12Med(),
            animated = true,
        )
    }
}
