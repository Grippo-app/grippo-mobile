package com.grippo.design.components.indicators

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor

@Composable
public fun LineIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    colors: AppColor.LineIndicatorColors.IndicatorColors = AppTokens.colors.lineIndicator.primary,
    cornerRadius: Dp = AppTokens.dp.contentPadding.text,
) {
    val shape = RoundedCornerShape(cornerRadius)

    LinearProgressIndicator(
        modifier = modifier.clip(shape),
        progress = { progress.coerceIn(0f, 1f) },
        color = colors.indicator,
        trackColor = colors.track
    )
}

@AppPreview
@Composable
private fun LineIndicatorPreview() {
    PreviewContainer {
        LineIndicator(
            progress = 0.6f,
        )
    }
}
