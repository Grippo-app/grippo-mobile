package com.grippo.network.dto.muscle

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
    @SerialName("recoveryTimeHours")
    val recoveryTimeHours: Int? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null
)