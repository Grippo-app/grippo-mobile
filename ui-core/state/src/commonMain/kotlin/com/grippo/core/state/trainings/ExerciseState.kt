package com.grippo.core.state.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.ImmutableListSerializer
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExampleValueState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.metrics.volume.stubTotal
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime
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
) {
    public fun maximumRecordedLoad(): Float? {
        val performed = iterations.filter { (it.repetitions.value ?: 0) > 0 }
        val loads = performed.map { it.recordedVolume() ?: return null }
        return loads.maxOrNull()
    }

    public fun volumeDeltaPercent(previous: ExerciseState): Double? {
        val current = total.volume.value ?: 0f
        val baseline = previous.total.volume.value ?: 0f
        if (baseline <= 0f || current < 0f) {
            return null
        }
        val delta = (current.toDouble() - baseline.toDouble()) / baseline.toDouble() * 100.0
        return delta.takeIf { it.isFinite() }
    }
}

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

public fun stubExerciseHistory(): PersistentList<ExerciseState> {
    val externalSet = stubPendingIteration().copy(
        id = "history-external-set",
        externalWeight = VolumeFormatState.of(80f),
        repetitions = RepetitionsFormatState.of(8),
    )
    val bodySet = stubPendingIteration().copy(
        id = "history-body-set",
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(1f),
        repetitions = RepetitionsFormatState.of(10),
    )

    return persistentListOf(
        stubExerciseHistoryEntry(
            id = "history-latest",
            day = 16,
            volume = 2_400f,
            iterations = persistentListOf(
                externalSet.copy(id = "history-latest-1"),
                externalSet.copy(id = "history-latest-2"),
                externalSet.copy(id = "history-latest-3", externalWeight = VolumeFormatState.of(85f)),
                stubPendingIteration().copy(id = "history-latest-pending", repetitions = RepetitionsFormatState.of(8)),
            ),
        ),
        stubExerciseHistoryEntry(
            id = "history-body",
            day = 12,
            volume = 2_200f,
            iterations = persistentListOf(
                bodySet.copy(id = "history-body-1"),
                bodySet.copy(id = "history-body-2", extraWeight = VolumeFormatState.of(20f)),
                bodySet.copy(id = "history-body-3", assistWeight = VolumeFormatState.of(30f)),
            ),
        ),
        stubExerciseHistoryEntry(
            id = "history-zero",
            day = 7,
            volume = 0f,
            iterations = persistentListOf(
                bodySet.copy(id = "history-zero-1", assistWeight = VolumeFormatState.of(80f)),
                bodySet.copy(id = "history-zero-2", assistWeight = VolumeFormatState.of(80f)),
            ),
        ),
        stubExerciseHistoryEntry(
            id = "history-oldest",
            day = 1,
            volume = 2_100f,
            iterations = persistentListOf(
                externalSet.copy(id = "history-oldest-1", externalWeight = VolumeFormatState.of(70f)),
                externalSet.copy(id = "history-oldest-2", externalWeight = VolumeFormatState.of(70f)),
                externalSet.copy(id = "history-oldest-3", externalWeight = VolumeFormatState.of(70f)),
            ),
        ),
    )
}

private fun stubExerciseHistoryEntry(
    id: String,
    day: Int,
    volume: Float,
    iterations: PersistentList<IterationState>,
): ExerciseState = stubExercise().copy(
    id = id,
    iterations = iterations,
    createdAt = DateTimeFormatState.of(
        value = LocalDateTime(year = 2026, month = 10, day = day, hour = 18, minute = 0),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    ),
    total = TrainingTotalState(
        volume = VolumeFormatState.of(volume),
        repetitions = RepetitionsFormatState.of(iterations.sumOf { it.repetitions.value ?: 0 }),
        intensity = IntensityFormatState.of(80f),
    ),
)
