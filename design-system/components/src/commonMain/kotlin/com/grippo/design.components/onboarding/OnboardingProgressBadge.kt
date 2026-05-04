package com.grippo.design.components.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.onboarding_progress_complete

@Composable
public fun OnboardingProgressBadge(
    modifier: Modifier = Modifier,
) {
    val accent = AppTokens.colors.semantic.success
    val track = AppTokens.colors.background.card
    val ringStroke = AppTokens.dp.onboarding.progress.ringStroke

    Row(
        modifier = modifier
            .background(color = track, shape = CircleShape)
            .padding(
                horizontal = AppTokens.dp.onboarding.progress.horizontalPadding,
                vertical = AppTokens.dp.onboarding.progress.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.onboarding.progress.space)
    ) {

        Canvas(modifier = Modifier.size(AppTokens.dp.onboarding.progress.ring)) {
            val stroke = Stroke(width = ringStroke.toPx())
            val inset = stroke.width / 2f
            val arcSize = Size(
                width = size.width - stroke.width,
                height = size.height - stroke.width
            )
            val topLeft = Offset(inset, inset)

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
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
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
