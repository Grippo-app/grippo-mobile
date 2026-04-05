package com.grippo.services.backend.dto.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class PushTokenBody(
    @SerialName("token")
    val token: String,
)
