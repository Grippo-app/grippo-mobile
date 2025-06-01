package com.grippo.design.components.toolbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun Toolbar(
    modifier: Modifier = Modifier,
    title: String,
) {
    Row(
        modifier = modifier
            .height(AppTokens.dp.screen.toolbar.height)
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@AppPreview
@Composable
private fun ToolbarPreview() {
    PreviewContainer {
        Toolbar(
            title = "Profile"
        )
    }
}