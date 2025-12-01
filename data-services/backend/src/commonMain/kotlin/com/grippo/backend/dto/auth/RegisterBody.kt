package com.grippo.backend.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class RegisterBody(
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String,
)
