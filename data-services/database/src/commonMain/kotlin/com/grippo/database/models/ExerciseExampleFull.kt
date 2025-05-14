package com.grippo.database.models

import androidx.room.Embedded
import androidx.room.Relation
import com.grippo.database.entity.EquipmentEntity
import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.database.entity.ExerciseExampleEquipmentEntity
import com.grippo.database.entity.ExerciseExampleTutorialEntity
import com.grippo.database.entity.MuscleEntity

public data class ExerciseExampleFull(
    @Embedded val example: ExerciseExampleEntity,

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

    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseExampleId",
        entity = ExerciseExampleTutorialEntity::class
    )
    val tutorials: List<ExerciseExampleTutorialEntity> = emptyList(),
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