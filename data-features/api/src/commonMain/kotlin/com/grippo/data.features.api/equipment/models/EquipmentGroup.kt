package com.grippo.data.features.api.equipment.models

public data class EquipmentGroup(
    val id: String,
    val name: String,
    val equipments: List<Equipment>
)