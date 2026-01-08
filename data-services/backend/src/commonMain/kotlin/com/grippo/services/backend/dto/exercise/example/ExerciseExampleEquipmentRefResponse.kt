package com.grippo.services.backend.dto.exercise.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ExerciseExampleEquipmentRefResponse(
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("equipment")
    val equipment: com.grippo.services.backend.dto.equipment.EquipmentResponse? = null,
    @SerialName("equipmentId")
    val equipmentId: String? = null,
    @SerialName("exerciseExampleId")
    val exerciseExampleId: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null
)