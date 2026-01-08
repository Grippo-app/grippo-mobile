package com.grippo.services.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipment_group")
public data class EquipmentGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val createdAt: String,
    val updatedAt: String,
)