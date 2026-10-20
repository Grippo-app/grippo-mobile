package com.grippo.start.training

import androidx.compose.runtime.Immutable
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlin.time.Duration.Companion.days
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Immutable
public data class StartTrainingState(
    val options: ImmutableList<StartTrainingOption> = persistentListOf(StartTrainingOption.Empty),
    val recentRange: DateRange = DateTimeUtils.now().let { now ->
        DateRange(
            from = DateTimeUtils.minus(value = now, minus = RECENT_TRAININGS_WINDOW_DAYS.days),
            to = now,
        )
    },
) {
    private companion object {
        private const val RECENT_TRAININGS_WINDOW_DAYS = 14
    }
}

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
        val createdAt: LocalDateTime,
        val exercises: ImmutableList<ExerciseState>,
    ) : StartTrainingOption {
        override val key: String = "recent:$trainingId"
    }
}
