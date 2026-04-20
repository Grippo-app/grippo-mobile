package com.grippo.domain.state.exercise.example

import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.domain.state.user.toState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets

public fun ExerciseExampleValue.toState(): ExerciseExampleValueState {
    return ExerciseExampleValueState(
        id = id,
        name = name,
        imageUrl = imageUrl,
        description = description,
        experience = experience.toState(),
        weightType = weightType.toState(),
        forceType = forceType.toState(),
        category = category.toState(),
        usageCount = usageCount,
        lastUsed = DateFormatState.of(
            value = lastUsed,
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateDdMmm
        )
    )
}
