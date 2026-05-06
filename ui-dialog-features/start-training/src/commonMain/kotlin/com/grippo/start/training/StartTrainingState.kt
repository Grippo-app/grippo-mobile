package com.grippo.start.training

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class StartTrainingState(
    val options: ImmutableList<StartTrainingOption> = persistentListOf(StartTrainingOption.Empty),
)

@Immutable
public sealed interface StartTrainingOption {
    public val key: String

    @Immutable
    public data object Empty : StartTrainingOption {
        override val key: String = "empty"
    }

    @Immutable
    public data class Preset(
        val exercises: ImmutableList<ExerciseState>,
    ) : StartTrainingOption {
        override val key: String = "preset"
    }

    @Immutable
    public data class Recent(
        val trainingId: String,
        val createdAt: DateTimeFormatState,
        val exercises: ImmutableList<ExerciseState>,
    ) : StartTrainingOption {
        override val key: String = "recent:$trainingId"
    }
}
