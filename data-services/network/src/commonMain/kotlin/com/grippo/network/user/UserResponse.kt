package com.grippo.network.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class UserResponse(
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("height")
    val height: Int? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("experience")
    val experience: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("weight")
    val weight: Float? = null
)