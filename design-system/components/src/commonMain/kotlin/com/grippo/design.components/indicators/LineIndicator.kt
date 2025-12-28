package com.grippo.design.components.indicators

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

    val coercedProgress = progress.coerceIn(0f, 1f)

    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(coercedProgress) {
        progressAnim.animateTo(
            targetValue = coercedProgress,
            animationSpec = tween(durationMillis = 300)
        )
    }
    val indicatorColor by animateColorAsState(
        targetValue = colors.indicator,
        animationSpec = tween(durationMillis = 300),
        label = "LineIndicatorIndicatorColor"
    )
    val trackColor by animateColorAsState(
        targetValue = colors.track,
        animationSpec = tween(durationMillis = 300),
        label = "LineIndicatorTrackColor"
    )

    LinearProgressIndicator(
        modifier = modifier.clip(shape),
        progress = { progressAnim.value },
        color = indicatorColor,
        trackColor = trackColor
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
