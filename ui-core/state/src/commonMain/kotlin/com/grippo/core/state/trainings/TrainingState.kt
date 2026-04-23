package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.ImmutableListSerializer
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.metrics.volume.stubTotal
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

@Immutable
@Serializable
public data class TrainingState(
    val id: String,
    val duration: DurationFormatState,
    val createdAt: DateFormatState,
    @Serializable(with = ImmutableListSerializer::class)
    val exercises: ImmutableList<ExerciseState>,
    val total: TrainingTotalState
)

public fun stubTraining(): TrainingState = TrainingState(
    id = Uuid.random().toString(),
    duration = DurationFormatState.of(75L.minutes),
    createdAt = DateFormatState.of(
        value = DateRangePresets.daily().from,
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy
    ),
    total = stubTotal(),
    exercises = listOf(
        stubExercise(),
        stubExercise(),
        stubExercise(),
        stubExercise(),
        stubExercise(),
        stubExercise()
    ).toPersistentList(),
)
