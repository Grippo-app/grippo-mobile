package com.grippo.design.components.divider

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun ContentDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = AppTokens.dp.divider.thickness,
        color = AppTokens.colors.divider.subtle,
    )
}

@AppPreview
@Composable
private fun ContentDividerPreview() {
    PreviewContainer {
        ContentDivider()
    }
}
