package com.grippo.backend.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class RegisterBody(
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String,
    @SerialName("name")
    val name: String,
    @SerialName("weight")
    val weight: Float,
    @SerialName("experience")
    val experience: String,
    @SerialName("height")
    val height: Int,
    @SerialName("excludeMuscleIds")
    val excludeMuscleIds: List<String>,
    @SerialName("excludeEquipmentIds")
    val excludeEquipmentIds: List<String>
)