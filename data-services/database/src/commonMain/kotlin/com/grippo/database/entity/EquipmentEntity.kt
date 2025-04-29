package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "equipment",
    foreignKeys = [
        ForeignKey(
            entity = EquipmentGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentGroupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
public data class EquipmentEntity(
    @PrimaryKey val id: String,
    val equipmentGroupId: String,
    val name: String,
    val status: String?,
    val type: String,
    val createdAt: String,
    val updatedAt: String,
)
