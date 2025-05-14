package com.grippo.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class AuthBody(
    @SerialName("email")
    val email: String? = null,
    @SerialName("password")
    val password: String? = null
)