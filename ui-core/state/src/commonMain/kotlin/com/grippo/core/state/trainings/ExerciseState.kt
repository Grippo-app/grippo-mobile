package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExampleValueState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
@Serializable
public data class ExerciseState(
    val id: String,
    val name: String,
    val iterations: ImmutableList<IterationState>,
    val exerciseExample: ExerciseExampleValueState,
    val metrics: TrainingMetrics
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
    metrics = stubMetrics(),
    exerciseExample = stubExerciseExampleValueState()
)