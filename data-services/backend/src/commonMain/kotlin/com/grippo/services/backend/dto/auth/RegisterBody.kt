package com.grippo.services.backend.dto.auth

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class RegisterBody(
    @SerialName("email")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val email: String? = null,
    @SerialName("password")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val password: String? = null,
)
