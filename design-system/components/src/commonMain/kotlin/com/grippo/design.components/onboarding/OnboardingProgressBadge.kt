package com.grippo.design.components.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.onboarding_progress_complete

@Composable
public fun OnboardingProgressBadge(
    modifier: Modifier = Modifier,
) {
    var target by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) { target = 1f }

    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = 900,
            easing = FastOutSlowInEasing
        ),
        label = "onboarding-progress"
    )

    val accent = AppTokens.colors.semantic.success
    val track = AppTokens.colors.background.card

    Row(
        modifier = modifier
            .background(
                color = track,
                shape = CircleShape
            )
            .padding(
                horizontal = AppTokens.dp.contentPadding.content,
                vertical = AppTokens.dp.contentPadding.subContent
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val stroke = Stroke(width = 3.dp.toPx())
                val padding = stroke.width / 2f
                val arcSize = Size(
                    width = size.width - stroke.width,
                    height = size.height - stroke.width
                )
                val topLeft = Offset(padding, padding)

                drawArc(
                    color = accent.copy(alpha = 0.18f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )

                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
        }

        Text(
            text = AppTokens.strings.res(Res.string.onboarding_progress_complete),
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@AppPreview
@Composable
private fun OnboardingProgressBadgePreview() {
    PreviewContainer {
        OnboardingProgressBadge()
    }
}
