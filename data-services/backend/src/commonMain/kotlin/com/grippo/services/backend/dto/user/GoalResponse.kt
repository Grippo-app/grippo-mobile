package com.grippo.services.backend.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class GoalResponse(
    @SerialName("primaryGoal")
    val primaryGoal: String? = null,
    @SerialName("secondaryGoal")
    val secondaryGoal: String? = null,
    @SerialName("target")
    val target: String? = null,
    @SerialName("personalizations")
    val personalizations: List<String>? = null,
    @SerialName("confidence")
    val confidence: Double? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("lastConfirmedAt")
    val lastConfirmedAt: String? = null,
)
