package com.grippo.state.domain.example

import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.state.domain.user.toDomain
import kotlinx.collections.immutable.toPersistentList

public fun List<ExerciseExampleValueState>.toDomain(): List<ExerciseExampleValue> {
    return mapNotNull { it.toDomain() }.toPersistentList()
}

public fun ExerciseExampleValueState.toDomain(): ExerciseExampleValue {
    return ExerciseExampleValue(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        experience = experience.toDomain(),
        forceType = forceType.toDomain(),
        weightType = weightType.toDomain(),
        category = category.toDomain(),
        usageCount = usageCount,
        lastUsed = lastUsed
    )
}
