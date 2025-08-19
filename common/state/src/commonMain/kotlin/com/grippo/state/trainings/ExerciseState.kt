package com.grippo.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.state.exercise.examples.ExerciseExampleValueState
import com.grippo.state.exercise.examples.stubExerciseExampleValueState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
public data class ExerciseState(
    val id: String,
    val name: String,
    val volume: VolumeFormatState,
    val repetitions: RepetitionsFormatState,
    val intensity: IntensityFormatState,
    val iterations: ImmutableList<IterationState>,
    val exerciseExample: ExerciseExampleValueState?,
)

public fun stubExercises(): PersistentList<ExerciseState> = persistentListOf(
    stubExercise(),
    stubExercise(),
    stubExercise(),
    stubExercise(),
)

public fun stubExercise(): ExerciseState = ExerciseState(
    id = Uuid.random().toString(),
    name = "Bench press",
    iterations = buildList {
        repeat(Random.nextInt(1, 12)) {
            add(stubIteration())
        }
    }.toPersistentList(),
    volume = VolumeFormatState.of(Random.nextInt(1000, 10000).toFloat()),
    intensity = IntensityFormatState.of(Random.nextInt(20, 100).toFloat()),
    repetitions = RepetitionsFormatState.of(Random.nextInt(20, 100)),
    exerciseExample = stubExerciseExampleValueState()
)