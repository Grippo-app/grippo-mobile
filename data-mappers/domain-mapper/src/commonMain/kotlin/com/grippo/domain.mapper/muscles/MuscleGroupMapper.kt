package com.grippo.domain.mapper.muscles

import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.presentation.api.muscles.models.MuscleGroupState
import com.grippo.presentation.api.muscles.models.MuscleRepresentationState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<MuscleGroup>.toState(): PersistentList<MuscleGroupState<MuscleRepresentationState.Plain>> {
    return map { it.toState() }.toPersistentList()
}

public fun MuscleGroup.toState(): MuscleGroupState<MuscleRepresentationState.Plain> {
    return MuscleGroupState(
        id = id,
        name = name,
        muscles = muscles.toState(),
        type = type.toState()
    )
}