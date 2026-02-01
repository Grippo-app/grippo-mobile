package com.grippo.services.database.models

import androidx.room.Embedded
import androidx.room.Relation
import com.grippo.services.database.entity.EquipmentEntity
import com.grippo.services.database.entity.ExerciseExampleBundleEntity
import com.grippo.services.database.entity.ExerciseExampleComponentsEntity
import com.grippo.services.database.entity.ExerciseExampleEntity
import com.grippo.services.database.entity.ExerciseExampleEquipmentEntity
import com.grippo.services.database.entity.MuscleEntity

public data class ExerciseExamplePack(
    @Embedded val example: ExerciseExampleEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseExampleId",
        entity = ExerciseExampleComponentsEntity::class
    )
    val components: ExerciseExampleComponentsEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseExampleId",
        entity = ExerciseExampleBundleEntity::class
    )
    val bundles: List<ExerciseExampleBundleWithMuscle> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseExampleId",
        entity = ExerciseExampleEquipmentEntity::class
    )
    val equipments: List<ExerciseEquipmentWithEquipment> = emptyList(),
)

public data class ExerciseExampleBundleWithMuscle(
    @Embedded val bundle: ExerciseExampleBundleEntity,

    @Relation(
        parentColumn = "muscleId",
        entityColumn = "id"
    )
    val muscle: MuscleEntity
)

public data class ExerciseEquipmentWithEquipment(
    @Embedded val equipmentRef: ExerciseExampleEquipmentEntity,

    @Relation(
        parentColumn = "equipmentId",
        entityColumn = "id"
    )
    val equipment: EquipmentEntity
)
