package com.grippo.training.recording

import com.grippo.state.trainings.IterationState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class TrainingRecordingState(
    val tab: RecordingTab = RecordingTab.Exercises,
    val exercises: ImmutableList<RecordingExerciseItem> = persistentListOf(),
)

internal enum class RecordingTab { Exercises, Stats }

internal data class RecordingExerciseItem(
    val id: String,
    val name: String,
    val iterations: ImmutableList<IterationState> = persistentListOf(),
)