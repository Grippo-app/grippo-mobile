package com.grippo.domain.state.muscles

import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.muscles.MuscleState
import com.grippo.data.features.api.muscle.models.Muscle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<Muscle>.toState(): ImmutableList<MuscleRepresentationState.Plain> {
    return map { it.toState() }.toPersistentList()
}

public fun Muscle.toState(): MuscleRepresentationState.Plain {
    val muscle = MuscleState(
        id = id,
        type = type.toState(),
    )

    return MuscleRepresentationState.Plain(muscle)
}