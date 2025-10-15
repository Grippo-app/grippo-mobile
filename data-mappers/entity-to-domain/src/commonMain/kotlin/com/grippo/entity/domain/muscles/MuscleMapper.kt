package com.grippo.entity.domain.muscles

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.database.entity.MuscleEntity
import com.grippo.toolkit.logger.AppLogger
import kotlin.time.Duration

public fun List<MuscleEntity>.toDomain(): List<Muscle> {
    return mapNotNull { it.toDomain() }
}

public fun MuscleEntity.toDomain(): Muscle? {
    val mappedType = AppLogger.Mapping.log(MuscleEnum.of(type)) {
        "MuscleEntity $id has unrecognized type: $type"
    } ?: return null

    return Muscle(
        id = id,
        name = name,
        recoveryTime = Duration.parse("${recoveryTimeHours}h"),
        type = mappedType
    )
}