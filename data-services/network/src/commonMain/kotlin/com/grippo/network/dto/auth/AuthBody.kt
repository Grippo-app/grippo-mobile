package com.grippo.network.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class AuthBody(
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String
)