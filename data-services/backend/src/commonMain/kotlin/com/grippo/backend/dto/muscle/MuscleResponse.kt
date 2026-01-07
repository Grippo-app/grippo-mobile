package com.grippo.backend.dto.muscle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class MuscleResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("muscleGroupId")
    val muscleGroupId: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("recovery")
    val recovery: Int? = null,
    @SerialName("size")
    val size: Float? = null,
    @SerialName("sensitivity")
    val sensitivity: Float? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null
)