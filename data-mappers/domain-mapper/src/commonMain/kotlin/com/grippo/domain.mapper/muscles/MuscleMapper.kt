package com.grippo.domain.mapper.muscles

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.presentation.api.muscles.models.MuscleRepresentationState
import com.grippo.presentation.api.muscles.models.MuscleState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<Muscle>.toState(): ImmutableList<MuscleRepresentationState.Plain> {
    return map { it.toState() }.toPersistentList()
}

public fun Muscle.toState(): MuscleRepresentationState.Plain {
    val muscle = MuscleState(
        id = id,
        name = name,
        type = type.toState(),
    )

    return MuscleRepresentationState.Plain(muscle)
}