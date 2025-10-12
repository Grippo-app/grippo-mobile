package com.grippo.backend.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class UserMuscleResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("muscleId")
    val muscleId: String? = null,
)