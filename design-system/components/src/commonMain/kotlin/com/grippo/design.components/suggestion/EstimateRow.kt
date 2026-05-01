package com.grippo.design.components.suggestion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Timer

@Composable
public fun EstimateRow(
    modifier: Modifier = Modifier,
    text: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.profile.goal.estimate.icon),
            imageVector = AppTokens.icons.Timer,
            tint = AppTokens.colors.text.tertiary,
            contentDescription = null,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            text = text,
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@AppPreview
@Composable
private fun EstimateRowPreview() {
    PreviewContainer {
        EstimateRow(
            modifier = Modifier.fillMaxWidth(),
            text = "Takes about a minute",
        )
    }
}
