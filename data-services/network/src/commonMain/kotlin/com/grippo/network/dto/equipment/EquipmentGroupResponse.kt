package com.grippo.network.dto.equipment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class EquipmentGroupResponse(
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("equipments")
    val equipments: List<EquipmentResponse>? = null
)