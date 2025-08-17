package com.grippo.training.recording

import androidx.compose.runtime.Immutable
import com.grippo.state.trainings.IterationState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class TrainingRecordingState(
    val tab: RecordingTab = RecordingTab.Exercises,
    val exercises: ImmutableList<RecordingExerciseItem> = persistentListOf(),
)

@Immutable
internal enum class RecordingTab { Exercises, Stats }

@Immutable
internal data class RecordingExerciseItem(
    val id: String,
    val name: String,
    val iterations: ImmutableList<IterationState> = persistentListOf(),
)