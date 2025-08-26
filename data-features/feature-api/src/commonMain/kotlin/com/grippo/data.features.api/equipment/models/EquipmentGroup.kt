package com.grippo.data.features.api.equipment.models

public data class EquipmentGroup(
    val id: String,
    val name: String,
    val type: EquipmentGroupEnum,
    val equipments: List<Equipment>
)