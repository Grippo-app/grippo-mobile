package com.grippo.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateTimeUtils
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
    val metrics: TrainingMetrics
)

public fun stubTraining(): TrainingState = TrainingState(
    id = Uuid.random().toString(),
    duration = 10000L.minutes,
    createdAt = DateTimeUtils.thisDay().from,
    metrics = stubMetrics(),
    exercises = listOf(stubExercise(), stubExercise(), stubExercise()).toPersistentList(),
)