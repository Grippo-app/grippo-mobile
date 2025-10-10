package com.grippo.data.features.api.muscle.models

import kotlin.time.Duration

public data class Muscle(
    val id: String,
    val name: String,
    val recoveryTime: Duration,
    val type: MuscleEnum
)