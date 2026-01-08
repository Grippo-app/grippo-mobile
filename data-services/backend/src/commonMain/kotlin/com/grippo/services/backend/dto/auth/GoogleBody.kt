package com.grippo.services.backend.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class GoogleBody(
    @SerialName("idToken")
    val idToken: String,
)
