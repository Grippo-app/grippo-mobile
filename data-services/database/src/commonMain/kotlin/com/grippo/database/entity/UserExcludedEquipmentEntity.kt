package com.grippo.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "user_excluded_equipment",
    primaryKeys = ["profileId", "equipmentId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId"),
        Index("equipmentId")
    ]
)
public data class UserExcludedEquipmentEntity(
    val profileId: String,
    val equipmentId: String,
)
