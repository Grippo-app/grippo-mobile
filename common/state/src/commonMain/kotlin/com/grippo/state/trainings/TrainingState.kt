package com.grippo.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
public data class TrainingState(
    val id: String,
    val duration: Long,
    val createdAt: LocalDateTime,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float,
    val exercises: ImmutableList<ExerciseState>
)

public fun stubTraining(): TrainingState = TrainingState(
    id = Uuid.random().toString(),
    duration = 10000L,
    createdAt = DateTimeUtils.thisDay().from,
    volume = Random.nextInt(1000, 10000).toFloat(),
    intensity = Random.nextInt(20, 100).toFloat(),
    repetitions = Random.nextInt(20, 100),
    exercises = listOf(stubExercise(), stubExercise(), stubExercise()).toPersistentList(),
)