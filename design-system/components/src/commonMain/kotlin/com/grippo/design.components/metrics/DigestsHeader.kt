package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.design.resources.provider.personal_digests

@Composable
public fun DigestsHeader(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.digests.icon),
            imageVector = AppTokens.icons.LineUp,
            tint = AppTokens.colors.icon.primary,
            contentDescription = null
        )

        Text(
            modifier = Modifier.weight(1f),
            text = AppTokens.strings.res(Res.string.personal_digests),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@AppPreview
@Composable
private fun DigestsHeaderPreview() {
    PreviewContainer {
        DigestsHeader()
    }
}
