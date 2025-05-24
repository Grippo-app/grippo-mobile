package com.grippo.domain.mapper

import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.muscles.models.MuscleGroupEnumState

public fun MuscleGroupEnum.toState(): MuscleGroupEnumState? {
    return when (this) {
        MuscleGroupEnum.CHEST_MUSCLES -> MuscleGroupEnumState.CHEST_MUSCLES
        MuscleGroupEnum.BACK_MUSCLES -> MuscleGroupEnumState.BACK_MUSCLES
        MuscleGroupEnum.ABDOMINAL_MUSCLES -> MuscleGroupEnumState.ABDOMINAL_MUSCLES
        MuscleGroupEnum.LEGS -> MuscleGroupEnumState.LEGS
        MuscleGroupEnum.ARMS_AND_FOREARMS -> MuscleGroupEnumState.ARMS_AND_FOREARMS
        MuscleGroupEnum.SHOULDER_MUSCLES -> MuscleGroupEnumState.SHOULDER_MUSCLES
        MuscleGroupEnum.UNIDENTIFIED -> AppLogger.checkOrLog(null) {
            "MuscleGroupEnum.UNIDENTIFIED cannot be mapped to state"
        }
    }
}