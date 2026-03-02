package com.grippo.services.backend.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class UserStatsResponse(
    @SerialName("trainingsCount")
    val trainingsCount: Int? = null,
    @SerialName("totalDuration")
    val totalDuration: Int? = null,
    @SerialName("totalVolume")
    val totalVolume: Float? = null,
    @SerialName("totalRepetitions")
    val totalRepetitions: Int? = null,
)
