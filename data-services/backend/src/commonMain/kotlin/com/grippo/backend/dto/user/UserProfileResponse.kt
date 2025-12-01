package com.grippo.backend.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class UserProfileResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("height")
    val height: Int? = null,
    @SerialName("experience")
    val experience: String? = null,
    @SerialName("weight")
    val weight: Float? = null,
)
