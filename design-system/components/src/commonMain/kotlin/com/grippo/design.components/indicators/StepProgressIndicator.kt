package com.grippo.design.components.indicators

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun StepProgressIndicator(
    modifier: Modifier = Modifier,
    stepCount: Int,
    currentStep: Int,
) {
    require(stepCount > 0)
    require(currentStep in 0 until stepCount)

    val colors = AppTokens.colors.stepProgressIndicator

    Row(
        modifier = modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = (currentStep + 1).toFloat(),
                range = 0f..stepCount.toFloat(),
                steps = stepCount - 1,
            )
        },
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.stepProgressIndicator.gap),
    ) {
        repeat(stepCount) { index ->
            val filled = index <= currentStep
            val fillStart by animateColorAsState(
                targetValue = if (filled) {
                    colors.fillStart
                } else {
                    colors.track
                },
                label = "step-progress-fill-start",
            )
            val fillEnd by animateColorAsState(
                targetValue = if (filled) {
                    colors.fillEnd
                } else {
                    colors.track
                },
                label = "step-progress-fill-end",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AppTokens.dp.stepProgressIndicator.height)
                    .background(
                        brush = Brush.horizontalGradient(listOf(fillStart, fillEnd)),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@AppPreview
@Composable
private fun StepProgressIndicatorStepsPreview() {
    PreviewContainer {
        repeat(4) { currentStep ->
            StepProgressIndicator(
                modifier = Modifier.fillMaxWidth(AppTokens.dp.screen.toolbar.progressWidthFraction),
                stepCount = 4,
                currentStep = currentStep,
            )
        }
    }
}

@AppPreview
@Composable
private fun StepProgressIndicatorSingleStepPreview() {
    PreviewContainer {
        StepProgressIndicator(
            modifier = Modifier.fillMaxWidth(AppTokens.dp.screen.toolbar.progressWidthFraction),
            stepCount = 1,
            currentStep = 0,
        )
    }
}
