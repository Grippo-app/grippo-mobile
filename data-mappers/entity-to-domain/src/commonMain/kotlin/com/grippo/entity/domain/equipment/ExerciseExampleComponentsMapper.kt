package com.grippo.entity.domain.equipment

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents
import com.grippo.services.database.entity.ExerciseExampleComponentsEntity
import com.grippo.toolkit.logger.AppLogger

public fun ExerciseExampleComponentsEntity?.toDomain(): ExerciseExampleComponents? {
    val entity = AppLogger.Mapping.log(this) {
        "ExerciseExampleComponentsEntity is null"
    } ?: return null

    val assistRequired = entity.assistWeightRequired
    val bodyRequired = entity.bodyWeightRequired
    val bodyMultiplier = entity.bodyWeightMultiplier
    val hasBody = !(bodyRequired == null && bodyMultiplier == null)
    val externalRequired = entity.externalWeightRequired
    val extraRequired = entity.extraWeightRequired

    if (externalRequired != null) {
        return ExerciseExampleComponents.External(externalRequired)
    }

    if (hasBody) {
        val mappedBodyRequired = AppLogger.Mapping.log(bodyRequired) {
            "ExerciseExampleComponentsEntity ${entity.exerciseExampleId} bodyWeightRequired is null"
        } ?: return null

        val mappedBodyMultiplier = AppLogger.Mapping.log(bodyMultiplier) {
            "ExerciseExampleComponentsEntity ${entity.exerciseExampleId} bodyWeightMultiplier is null"
        } ?: return null

        return when {
            extraRequired != null -> ExerciseExampleComponents.BodyAndExtra(
                bodyRequired = mappedBodyRequired,
                bodyMultiplier = mappedBodyMultiplier,
                extraRequired = extraRequired
            )

            assistRequired != null -> ExerciseExampleComponents.BodyAndAssist(
                bodyRequired = mappedBodyRequired,
                bodyMultiplier = mappedBodyMultiplier,
                assistRequired = assistRequired
            )

            else -> ExerciseExampleComponents.BodyOnly(
                required = mappedBodyRequired,
                multiplier = mappedBodyMultiplier
            )
        }
    }

    AppLogger.Mapping.log(Unit) {
        "ExerciseExampleComponentsEntity ${entity.exerciseExampleId} has no components data"
    }
    return null
}
