package com.grippo.services.backend.dto.user

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class SetGoalBody(
    @SerialName("primaryGoal")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val primaryGoal: String? = null,
    @SerialName("secondaryGoal")
    val secondaryGoal: String? = null,
    @SerialName("target")
    val target: String? = null,
    @SerialName("personalizations")
    val personalizations: List<String>? = null,
)
