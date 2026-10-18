package com.grippo.draft.training

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class DraftTrainingState(
    val stage: StageState = StageState.Draft,
    val exercises: ImmutableList<ExerciseState> = persistentListOf(),
    val duration: DurationFormatState = DurationFormatState.Empty(),
)
