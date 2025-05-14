package com.grippo.database.models

import androidx.room.Embedded
import androidx.room.Relation
import com.grippo.database.entity.UserEntity
import com.grippo.database.entity.UserExcludedEquipmentEntity
import com.grippo.database.entity.UserExcludedMuscleEntity

public data class UserFull(
    @Embedded
    val user: UserEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "userId",
        entity = UserExcludedMuscleEntity::class
    )
    val excludedMuscles: List<UserExcludedMuscleEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "userId",
        entity = UserExcludedEquipmentEntity::class
    )
    val excludedEquipments: List<UserExcludedEquipmentEntity>
)