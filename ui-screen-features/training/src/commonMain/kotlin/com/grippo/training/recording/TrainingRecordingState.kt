package com.grippo.training.recording

import androidx.compose.runtime.Immutable
import com.grippo.state.trainings.TrainingState

@Immutable
internal data class TrainingRecordingState(
    val tab: RecordingTab = RecordingTab.Exercises,
    val training: TrainingState? = null
)

@Immutable
internal enum class RecordingTab {
    Exercises,
    Stats
}
