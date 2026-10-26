package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState

@Immutable
internal interface TrainingCompletedContract {
    fun onShareClick(capture: suspend () -> ImageBitmap)
    fun onRecordClick(exampleId: String)
    fun onReferenceSelect(reference: PerformanceReferenceState)
    fun onMetricClick(type: PerformanceMetricTypeState)
    fun onStreakClick()
    fun onGoalClick()
    fun onRecordConfettiShown()
    fun onBack()

    @Immutable
    companion object Empty : TrainingCompletedContract {
        override fun onShareClick(capture: suspend () -> ImageBitmap) {}
        override fun onRecordClick(exampleId: String) {}
        override fun onReferenceSelect(reference: PerformanceReferenceState) {}
        override fun onMetricClick(type: PerformanceMetricTypeState) {}
        override fun onStreakClick() {}
        override fun onGoalClick() {}
        override fun onRecordConfettiShown() {}
        override fun onBack() {}
    }
}
