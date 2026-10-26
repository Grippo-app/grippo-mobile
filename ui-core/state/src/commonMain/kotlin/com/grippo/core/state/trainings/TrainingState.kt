package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.ImmutableListSerializer
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.metrics.volume.stubTotal
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

@Immutable
@Serializable
public data class TrainingState(
    val id: String,
    val duration: DurationFormatState,
    val createdAt: DateTimeFormatState,
    @Serializable(with = ImmutableListSerializer::class)
    val exercises: ImmutableList<ExerciseState>,
    val total: TrainingTotalState
)

public fun stubTraining(): TrainingState = TrainingState(
    id = Uuid.random().toString(),
    duration = DurationFormatState.of(75L.minutes),
    createdAt = DateTimeFormatState.of(
        value = DateRangePresets.daily().from,
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateDdMmmYyyy
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

public fun stubTrainingSummary(): TrainingState {
    val training = stubTraining()
    val iteration = stubPendingIteration().copy(
        id = "summary-set",
        repetitions = RepetitionsFormatState.of(10),
        externalWeight = VolumeFormatState.of(40f),
    )
    val exercises = training.exercises.take(3).mapIndexed { index, exercise ->
        exercise.copy(
            id = "summary-exercise-$index",
            iterations = List(3) { setIndex ->
                iteration.copy(id = "summary-set-$index-$setIndex")
            }.toPersistentList(),
        )
    }.toPersistentList()

    return training.copy(
        id = "summary-training",
        createdAt = DateTimeFormatState.of(
            value = LocalDateTime(2026, 10, 18, 12, 0),
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateDdMmmYyyy,
        ),
        duration = DurationFormatState.ofTotal(75.minutes),
        exercises = exercises,
        total = training.total.copy(
            volume = VolumeFormatState.ofTotal(3_600f),
            repetitions = RepetitionsFormatState.ofTotal(90),
        ),
    )
}
