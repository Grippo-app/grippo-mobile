package com.grippo.design.components.spliter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun ContentSpliter(
    modifier: Modifier = Modifier,
    text: String
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = AppTokens.colors.divider.default
        )

        Text(
            text = text,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.tertiary,
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = AppTokens.colors.divider.default
        )
    }
}

@AppPreview
@Composable
private fun ContentSpliterPreview() {
    PreviewContainer {
        ContentSpliter(
            text = "Results"
        )
    }
}