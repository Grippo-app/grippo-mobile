package com.grippo.data.features.api.muscle.models

public enum class MuscleEnum(private val key: String) {
    // Chest
    PECTORALIS_MAJOR_CLAVICULAR("pectoralis_major_clavicular"),
    PECTORALIS_MAJOR_STERNOCOSTAL("pectoralis_major_sternocostal"),
    PECTORALIS_MAJOR_ABDOMINAL("pectoralis_major_abdominal"),

    // Back
    TRAPEZIUS("trapezius"),
    LATISSIMUS_DORSI("latissimus_dorsi"),
    RHOMBOIDS("rhomboids"),
    TERES_MAJOR("teres_major"),

    // Abdominal
    RECTUS_ABDOMINIS("rectus_abdominis"),
    OBLIQUES("obliques"),

    // Legs
    CALF("calf"),
    GLUTEAL("gluteal"),
    HAMSTRINGS("hamstrings"),
    QUADRICEPS("quadriceps"),
    ADDUCTORS("adductors"),
    ABDUCTORS("abductors"),

    // Shoulder
    ANTERIOR_DELTOID("anterior_deltoid"),
    LATERAL_DELTOID("lateral_deltoid"),
    POSTERIOR_DELTOID("posterior_deltoid"),

    // Arms
    BICEPS("biceps"),
    TRICEPS("triceps"),
    FOREARM("forearm");

    public fun group(): MuscleGroupEnum = when (this) {
        PECTORALIS_MAJOR_CLAVICULAR,
        PECTORALIS_MAJOR_STERNOCOSTAL,
        PECTORALIS_MAJOR_ABDOMINAL,
            -> MuscleGroupEnum.CHEST_MUSCLES

        TRAPEZIUS,
        LATISSIMUS_DORSI,
        RHOMBOIDS,
        TERES_MAJOR,
            -> MuscleGroupEnum.BACK_MUSCLES

        RECTUS_ABDOMINIS,
        OBLIQUES,
            -> MuscleGroupEnum.ABDOMINAL_MUSCLES

        CALF,
        GLUTEAL,
        HAMSTRINGS,
        QUADRICEPS,
        ADDUCTORS,
        ABDUCTORS,
            -> MuscleGroupEnum.LEGS

        ANTERIOR_DELTOID,
        LATERAL_DELTOID,
        POSTERIOR_DELTOID,
            -> MuscleGroupEnum.SHOULDER_MUSCLES

        BICEPS,
        TRICEPS,
        FOREARM,
            -> MuscleGroupEnum.ARMS_AND_FOREARMS
    }

    public companion object {
        public fun of(key: String): MuscleEnum? {
            return entries.firstOrNull { it.key == key }
        }
    }
}