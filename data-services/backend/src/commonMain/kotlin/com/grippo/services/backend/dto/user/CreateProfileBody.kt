package com.grippo.services.backend.dto.user

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class CreateProfileBody(
    @SerialName("name")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val name: String? = null,
    @SerialName("weight")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val weight: Float? = null,
    @SerialName("height")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val height: Int? = null,
    @SerialName("experience")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val experience: String? = null,
    @SerialName("excludeMuscleIds")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val excludeMuscleIds: List<String>? = null,
    @SerialName("excludeEquipmentIds")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val excludeEquipmentIds: List<String>? = null,
)
