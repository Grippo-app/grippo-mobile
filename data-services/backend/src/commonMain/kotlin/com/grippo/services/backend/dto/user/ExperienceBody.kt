package com.grippo.services.backend.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ExperienceBody(
    @SerialName("experience")
    val experience: String,
)
