package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import com.grippo.core.state.metrics.performance.PerformanceBaselineState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState

@Immutable
internal interface TrainingCompletedContract {
    fun onShareClick(capture: suspend () -> ImageBitmap)
    fun onRecordClick(exampleId: String)
    fun onBaselineSelect(baseline: PerformanceBaselineState)
    fun onMetricClick(type: PerformanceMetricTypeState)
    fun onStreakClick()
    fun onGoalClick()
    fun onRecordConfettiShown()
    fun onBack()

    @Immutable
    companion object Empty : TrainingCompletedContract {
        override fun onShareClick(capture: suspend () -> ImageBitmap) {}
        override fun onRecordClick(exampleId: String) {}
        override fun onBaselineSelect(baseline: PerformanceBaselineState) {}
        override fun onMetricClick(type: PerformanceMetricTypeState) {}
        override fun onStreakClick() {}
        override fun onGoalClick() {}
        override fun onRecordConfettiShown() {}
        override fun onBack() {}
    }
}
