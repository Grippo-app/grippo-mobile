package com.grippo.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.state.exercise.examples.ExerciseExampleValueState
import com.grippo.state.exercise.examples.stubExerciseExampleValueState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
public data class ExerciseState(
    val id: String,
    val name: String,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float,
    val iterations: ImmutableList<IterationState>,
    val exerciseExample: ExerciseExampleValueState?,
)

public fun stubExercise(): ExerciseState = ExerciseState(
    id = Uuid.random().toString(),
    name = "Bench press",
    iterations = buildList {
        repeat(Random.nextInt(1, 12)) {
            add(stubIteration())
        }
    }.toPersistentList(),
    volume = Random.nextInt(1000, 10000).toFloat(),
    intensity = Random.nextInt(20, 100).toFloat(),
    repetitions = Random.nextInt(20, 100),
    exerciseExample = stubExerciseExampleValueState()
)