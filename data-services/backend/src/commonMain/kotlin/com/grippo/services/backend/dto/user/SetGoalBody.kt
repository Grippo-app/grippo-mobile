package com.grippo.services.backend.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class SetGoalBody(
    @SerialName("primaryGoal")
    val primaryGoal: String,
    @SerialName("secondaryGoal")
    val secondaryGoal: String? = null,
    @SerialName("target")
    val target: String? = null,
    @SerialName("personalizations")
    val personalizations: List<String> = emptyList(),
)
