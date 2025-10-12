package com.grippo.backend.dto.exercise.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class GetExerciseExampleResponse(
    @SerialName("entity")
    val entity: ExerciseExampleResponse? = null,
    @SerialName("usageCount")
    val usageCount: Int? = null,
    @SerialName("lastUsed")
    val lastUsed: String? = null,
)