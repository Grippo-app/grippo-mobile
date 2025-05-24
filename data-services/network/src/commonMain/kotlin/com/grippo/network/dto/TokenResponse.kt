package com.grippo.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class TokenResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("accessToken")
    val accessToken: String? = null,
    @SerialName("refreshToken")
    val refreshToken: String? = null,
)