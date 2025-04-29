package com.grippo.database.models

import androidx.room.Embedded
import androidx.room.Relation
import com.grippo.database.entity.MuscleEntity
import com.grippo.database.entity.MuscleGroupEntity

public data class MuscleGroupWithMuscles(
    @Embedded val group: MuscleGroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "muscleGroupId"
    )
    val muscles: List<MuscleEntity>
)