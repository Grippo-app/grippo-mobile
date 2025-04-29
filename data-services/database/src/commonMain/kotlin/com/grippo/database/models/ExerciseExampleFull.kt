package com.grippo.database.models

import androidx.room.Embedded
import androidx.room.Relation
import com.grippo.database.entity.EquipmentEntity
import com.grippo.database.entity.ExerciseEquipmentEntity
import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.database.entity.ExerciseTutorialEntity
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
        entity = ExerciseEquipmentEntity::class
    )
    val equipments: List<ExerciseEquipmentWithEquipment> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseExampleId",
        entity = ExerciseTutorialEntity::class
    )
    val tutorials: List<ExerciseTutorialEntity> = emptyList(),
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
    @Embedded val equipmentRef: ExerciseEquipmentEntity,

    @Relation(
        parentColumn = "equipmentId",
        entityColumn = "id"
    )
    val equipment: EquipmentEntity
)