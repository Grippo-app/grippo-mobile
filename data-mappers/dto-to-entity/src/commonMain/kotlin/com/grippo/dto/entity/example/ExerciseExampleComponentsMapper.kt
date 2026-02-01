package com.grippo.dto.entity.example

import com.grippo.services.backend.dto.exercise.example.ExerciseExampleComponentsResponse
import com.grippo.services.database.entity.ExerciseExampleComponentsEntity

public fun ExerciseExampleComponentsResponse?.toEntity(
    exerciseExampleId: String
): ExerciseExampleComponentsEntity {
    return ExerciseExampleComponentsEntity(
        exerciseExampleId = exerciseExampleId,
        assistWeightRequired = this?.assistWeight?.required,
        bodyWeightRequired = this?.bodyWeight?.required,
        bodyWeightMultiplier = this?.bodyWeight?.multiplier,
        externalWeightRequired = this?.externalWeight?.required,
        extraWeightRequired = this?.extraWeight?.required,
    )
}
