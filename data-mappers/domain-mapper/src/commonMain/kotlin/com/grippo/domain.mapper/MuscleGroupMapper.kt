package com.grippo.domain.mapper

import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.presentation.api.muscles.models.MuscleGroupEnumState
import com.grippo.presentation.api.muscles.models.MuscleGroupState
import com.grippo.presentation.api.muscles.models.MuscleRepresentation
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<MuscleGroup>.toState(): PersistentList<MuscleGroupState<MuscleRepresentation.Plain>> {
    return mapNotNull { it.toState() }.toPersistentList()
}

public fun MuscleGroup.toState(): MuscleGroupState<MuscleRepresentation.Plain>? {
    return MuscleGroupState<MuscleRepresentation.Plain>(
        id = id,
        name = name,
        muscles = muscles.toState(),
        type = type.toState() ?: return null
    )
}

public fun MuscleGroupEnum.toState(): MuscleGroupEnumState? {
    return when (this) {
        MuscleGroupEnum.CHEST_MUSCLES -> MuscleGroupEnumState.CHEST_MUSCLES
        MuscleGroupEnum.BACK_MUSCLES -> MuscleGroupEnumState.BACK_MUSCLES
        MuscleGroupEnum.ABDOMINAL_MUSCLES -> MuscleGroupEnumState.ABDOMINAL_MUSCLES
        MuscleGroupEnum.LEGS -> MuscleGroupEnumState.LEGS
        MuscleGroupEnum.ARMS_AND_FOREARMS -> MuscleGroupEnumState.ARMS_AND_FOREARMS
        MuscleGroupEnum.SHOULDER_MUSCLES -> MuscleGroupEnumState.SHOULDER_MUSCLES
        MuscleGroupEnum.UNIDENTIFIED -> null
    }
}