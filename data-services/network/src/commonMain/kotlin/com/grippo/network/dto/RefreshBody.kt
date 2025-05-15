package com.grippo.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class RefreshBody(
    @SerialName("refreshToken")
    val refreshToken: String? = null,
)