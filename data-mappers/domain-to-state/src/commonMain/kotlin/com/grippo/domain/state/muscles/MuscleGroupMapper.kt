package com.grippo.domain.state.muscles

import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.data.features.api.muscle.models.MuscleGroup
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<MuscleGroup>.toState(): PersistentList<MuscleGroupState<MuscleRepresentationState.Plain>> {
    return map { it.toState() }.toPersistentList()
}

public fun MuscleGroup.toState(): MuscleGroupState<MuscleRepresentationState.Plain> {
    return MuscleGroupState(
        id = id,
        muscles = muscles.toState(),
        type = type.toState()
    )
}