package com.grippo.domain.mapper

import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.muscles.models.MuscleEnumState
import com.grippo.presentation.api.muscles.models.MuscleRepresentationState
import com.grippo.presentation.api.muscles.models.MuscleState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<Muscle>.toState(): ImmutableList<MuscleRepresentationState.Plain> {
    return mapNotNull { it.toState() }.toPersistentList()
}

public fun Muscle.toState(): MuscleRepresentationState.Plain? {
    val mappedType = AppLogger.checkOrLog(type.toState()) {
        "Muscle $id has unrecognized type: $type"
    } ?: return null

    val muscle = MuscleState(
        id = id,
        name = name,
        type = mappedType,
    )

    return MuscleRepresentationState.Plain(muscle)
}

public fun MuscleEnum.toState(): MuscleEnumState? {
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
        MuscleEnum.UNIDENTIFIED -> AppLogger.checkOrLog(null) {
            "MuscleEnum.UNIDENTIFIED cannot be mapped to state"
        }
    }
}