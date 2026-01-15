package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.TrainingTotalState
import com.grippo.core.state.metrics.stubTotal
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

@Immutable
@Serializable
public data class TrainingState(
    val id: String,
    val duration: Duration,
    val createdAt: LocalDateTime,
    val exercises: ImmutableList<ExerciseState>,
    val total: TrainingTotalState
)

public fun stubTraining(): TrainingState = TrainingState(
    id = Uuid.random().toString(),
    duration = 10000L.minutes,
    createdAt = DateRange.Range.Daily().range.from,
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
