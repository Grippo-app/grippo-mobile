package com.grippo.services.backend.dto.auth

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class RefreshBody(
    @SerialName("refreshToken")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val refreshToken: String? = null,
)