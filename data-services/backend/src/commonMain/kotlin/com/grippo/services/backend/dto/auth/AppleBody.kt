package com.grippo.services.backend.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class AppleBody(
    @SerialName("idToken")
    val idToken: String,
    @SerialName("authorizationCode")
    val authorizationCode: String,
)
