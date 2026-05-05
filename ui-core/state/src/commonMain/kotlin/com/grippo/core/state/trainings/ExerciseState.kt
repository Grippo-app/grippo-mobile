package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.ImmutableListSerializer
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExampleValueState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.metrics.volume.stubTotal
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
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
    @Serializable(with = ImmutableListSerializer::class)
    val iterations: ImmutableList<IterationState>,
    val exerciseExample: ExerciseExampleValueState,
    val createdAt: DateTimeFormatState,
    val total: TrainingTotalState
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
    total = stubTotal(),
    exerciseExample = stubExerciseExampleValueState(),
    createdAt = DateTimeFormatState.of(
        value = DateTimeUtils.now(),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy
    )
)


public fun stubPendingExercise(): ExerciseState = ExerciseState(
    id = Uuid.random().toString(),
    name = "Bench press",
    iterations = buildList {
        repeat(Random.nextInt(1, 12)) {
            add(stubPendingIteration())
        }
    }.toPersistentList(),
    total = stubTotal(),
    exerciseExample = stubExerciseExampleValueState(),
    createdAt = DateTimeFormatState.of(
        value = DateTimeUtils.now(),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy
    )
)
