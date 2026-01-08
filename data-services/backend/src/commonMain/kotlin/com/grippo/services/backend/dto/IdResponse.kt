package com.grippo.services.backend.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class IdResponse(
    @SerialName("id")
    val id: String? = null
)
