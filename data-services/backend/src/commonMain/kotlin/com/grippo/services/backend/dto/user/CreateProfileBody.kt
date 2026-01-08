package com.grippo.services.backend.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class CreateProfileBody(
    @SerialName("name")
    val name: String,
    @SerialName("weight")
    val weight: Float,
    @SerialName("height")
    val height: Int,
    @SerialName("experience")
    val experience: String,
    @SerialName("excludeMuscleIds")
    val excludeMuscleIds: List<String>?,
    @SerialName("excludeEquipmentIds")
    val excludeEquipmentIds: List<String>?,
)
