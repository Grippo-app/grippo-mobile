package com.grippo.services.database.models

import androidx.room.Embedded
import androidx.room.Relation
import com.grippo.services.database.entity.EquipmentEntity
import com.grippo.services.database.entity.EquipmentGroupEntity

public data class EquipmentGroupWithEquipments(
    @Embedded val group: EquipmentGroupEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "equipmentGroupId"
    )
    val equipments: List<EquipmentEntity>
)