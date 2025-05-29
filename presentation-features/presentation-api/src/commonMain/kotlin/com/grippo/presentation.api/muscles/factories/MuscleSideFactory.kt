package com.grippo.presentation.api.muscles.factories

import com.grippo.presentation.api.muscles.models.MuscleEnumState

internal enum class BodySide {
    Front, Back, Both
}

internal object MuscleSideFactory {

    fun side(muscle: MuscleEnumState): BodySide = when (muscle) {
        MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
        MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL,
        MuscleEnumState.PECTORALIS_MAJOR_ABDOMINAL,
        MuscleEnumState.RECTUS_ABDOMINIS,
        MuscleEnumState.OBLIQUES,
        MuscleEnumState.ANTERIOR_DELTOID,
        MuscleEnumState.LATERAL_DELTOID,
        MuscleEnumState.QUADRICEPS,
        MuscleEnumState.BICEPS -> BodySide.Front

        MuscleEnumState.TRAPEZIUS,
        MuscleEnumState.LATISSIMUS_DORSI,
        MuscleEnumState.RHOMBOIDS,
        MuscleEnumState.TERES_MAJOR,
        MuscleEnumState.POSTERIOR_DELTOID,
        MuscleEnumState.GLUTEAL,
        MuscleEnumState.HAMSTRINGS,
        MuscleEnumState.CALF,
        MuscleEnumState.ADDUCTORS,
        MuscleEnumState.ABDUCTORS,
        MuscleEnumState.TRICEPS -> BodySide.Back

        MuscleEnumState.FOREARM -> BodySide.Both
    }
}