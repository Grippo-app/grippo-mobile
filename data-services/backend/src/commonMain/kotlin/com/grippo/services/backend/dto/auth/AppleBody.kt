package com.grippo.services.backend.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class AppleBody(
    @SerialName("code")
    val code: String,
)
