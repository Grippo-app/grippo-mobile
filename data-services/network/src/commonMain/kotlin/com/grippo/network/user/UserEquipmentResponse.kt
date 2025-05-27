package com.grippo.network.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class UserEquipmentResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("equipmentId")
    val equipmentId: String? = null,
)