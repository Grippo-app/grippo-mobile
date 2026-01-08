package com.grippo.services.backend.dto.exercise.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ExerciseExampleBundleResponse(
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("exerciseExampleId")
    val exerciseExampleId: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("muscle")
    val muscle: com.grippo.services.backend.dto.muscle.MuscleResponse? = null,
    @SerialName("muscleId")
    val muscleId: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("percentage")
    val percentage: Int? = null
)