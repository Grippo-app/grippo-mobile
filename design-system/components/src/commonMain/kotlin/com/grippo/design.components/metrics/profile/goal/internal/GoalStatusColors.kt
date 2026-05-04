package com.grippo.design.components.metrics.profile.goal.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.AppColor

@Composable
internal fun goalStatusColor(
    score: Int,
    isFinished: Boolean,
): Color = when {
    isFinished && score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.semantic.success
    score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.semantic.success
    score >= GoalProgressState.DRIFTING_MIN -> AppTokens.colors.semantic.warning
    else -> AppTokens.colors.semantic.error
}

@Composable
internal fun goalRingColors(
    score: Int,
    isFinished: Boolean,
): AppColor.Charts.RingColor.RingPalette = when {
    isFinished && score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.charts.ring.success
    score >= GoalProgressState.ON_TRACK_MIN -> AppTokens.colors.charts.ring.success
    score >= GoalProgressState.DRIFTING_MIN -> AppTokens.colors.charts.ring.warning
    else -> AppTokens.colors.charts.ring.error
}