package com.grippo.domain.mapper.muscles

import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.presentation.api.muscles.models.MuscleEnumState

public fun MuscleEnum.toState(): MuscleEnumState {
    return when (this) {
        MuscleEnum.PECTORALIS_MAJOR_ABDOMINAL -> MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL
        MuscleEnum.PECTORALIS_MAJOR_CLAVICULAR -> MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR
        MuscleEnum.PECTORALIS_MAJOR_STERNOCOSTAL -> MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL
        MuscleEnum.TRAPEZIUS -> MuscleEnumState.TRAPEZIUS
        MuscleEnum.LATISSIMUS_DORSI -> MuscleEnumState.LATISSIMUS_DORSI
        MuscleEnum.RHOMBOIDS -> MuscleEnumState.RHOMBOIDS
        MuscleEnum.RECTUS_ABDOMINIS -> MuscleEnumState.RECTUS_ABDOMINIS
        MuscleEnum.OBLIQUES -> MuscleEnumState.OBLIQUES
        MuscleEnum.TERES_MAJOR -> MuscleEnumState.TERES_MAJOR
        MuscleEnum.CALF -> MuscleEnumState.CALF
        MuscleEnum.GLUTEAL -> MuscleEnumState.GLUTEAL
        MuscleEnum.HAMSTRINGS -> MuscleEnumState.HAMSTRINGS
        MuscleEnum.QUADRICEPS -> MuscleEnumState.QUADRICEPS
        MuscleEnum.ANTERIOR_DELTOID -> MuscleEnumState.ANTERIOR_DELTOID
        MuscleEnum.LATERAL_DELTOID -> MuscleEnumState.LATERAL_DELTOID
        MuscleEnum.POSTERIOR_DELTOID -> MuscleEnumState.POSTERIOR_DELTOID
        MuscleEnum.BICEPS -> MuscleEnumState.BICEPS
        MuscleEnum.TRICEPS -> MuscleEnumState.TRICEPS
        MuscleEnum.FOREARM -> MuscleEnumState.FOREARM
        MuscleEnum.ADDUCTORS -> MuscleEnumState.ADDUCTORS
        MuscleEnum.ABDUCTORS -> MuscleEnumState.ABDUCTORS
    }
}