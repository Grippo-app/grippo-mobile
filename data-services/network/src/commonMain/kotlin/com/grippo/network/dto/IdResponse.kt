package com.grippo.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class IdResponse(
    @SerialName("id")
    val id: String? = null
)
