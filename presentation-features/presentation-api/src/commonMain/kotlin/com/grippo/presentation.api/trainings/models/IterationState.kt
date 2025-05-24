package com.grippo.presentation.api.trainings.models

import androidx.compose.runtime.Immutable
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
public data class IterationState(
    val id: String,
    val weight: Float,
    val repetitions: Int,
)

public fun stubIteration(): IterationState = IterationState(
    id = Uuid.random().toString(),
    weight = Random.nextInt(40, 250).toFloat(),
    repetitions = Random.nextInt(2, 16)
)