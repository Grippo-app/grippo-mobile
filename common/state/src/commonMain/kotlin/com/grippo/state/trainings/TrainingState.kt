package com.grippo.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateTimeUtils
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
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
    val volume: VolumeFormatState,
    val repetitions: RepetitionsFormatState,
    val intensity: IntensityFormatState,
    val exercises: ImmutableList<ExerciseState>
)

public fun stubTraining(): TrainingState = TrainingState(
    id = Uuid.random().toString(),
    duration = 10000L,
    createdAt = DateTimeUtils.thisDay().from,
    volume = VolumeFormatState.of(Random.nextInt(1000, 10000).toFloat()),
    intensity = IntensityFormatState.of(Random.nextInt(20, 100).toFloat()),
    repetitions = RepetitionsFormatState.of(Random.nextInt(20, 100)),
    exercises = listOf(stubExercise(), stubExercise(), stubExercise()).toPersistentList(),
)