package com.grippo.start.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.start_training_option_preset_title
import com.grippo.design.resources.provider.start_training_option_recent_title
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class StartTrainingState(
    val options: ImmutableList<StartTrainingOption> = persistentListOf(StartTrainingOption.Empty),
)

@Immutable
public sealed interface StartTrainingOption {
    public val key: String

    @Composable
    public fun hint(): String?

    @Immutable
    public data object Empty : StartTrainingOption {
        override val key: String = "empty"

        @Composable
        override fun hint(): String? {
            return null
        }
    }

    @Immutable
    public data class Preset(
        val exercises: ImmutableList<ExerciseState>,
    ) : StartTrainingOption {
        override val key: String = "preset"

        @Composable
        override fun hint(): String {
            return AppTokens.strings.res(Res.string.start_training_option_preset_title)
        }
    }

    @Immutable
    public data class Recent(
        val trainingId: String,
        val createdAt: DateTimeFormatState,
        val exercises: ImmutableList<ExerciseState>,
    ) : StartTrainingOption {
        override val key: String = "recent:$trainingId"

        @Composable
        override fun hint(): String {
            return AppTokens.strings.res(Res.string.start_training_option_recent_title)
        }
    }
}
