package com.grippo.services.backend.dto.user

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ExperienceBody(
    @SerialName("experience")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val experience: String? = null,
)
