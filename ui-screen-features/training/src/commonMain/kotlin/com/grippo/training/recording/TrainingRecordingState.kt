package com.grippo.training.recording

import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateTimeUtils
import com.grippo.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Immutable
internal data class TrainingRecordingState(
    val tab: RecordingTab = RecordingTab.Exercises,
    val exercises: ImmutableList<ExerciseState> = persistentListOf(),
    val startAt: LocalDateTime = DateTimeUtils.now()
)

@Immutable
internal enum class RecordingTab {
    Exercises,
    Stats
}
