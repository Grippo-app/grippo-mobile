package com.grippo.state.muscles

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.provider.icons.muscles.MuscleColorPreset

@Immutable
public enum class MuscleEnumState {
    // Chest
    PECTORALIS_MAJOR_CLAVICULAR,
    PECTORALIS_MAJOR_STERNOCOSTAL,
    PECTORALIS_MAJOR_ABDOMINAL,

    // Back
    TRAPEZIUS,
    LATISSIMUS_DORSI,
    RHOMBOIDS,
    TERES_MAJOR,

    // Abdominal
    RECTUS_ABDOMINIS,
    OBLIQUES,

    // Legs
    CALF,
    GLUTEAL,
    HAMSTRINGS,
    QUADRICEPS,
    ADDUCTORS,
    ABDUCTORS,

    // Shoulder
    ANTERIOR_DELTOID,
    LATERAL_DELTOID,
    POSTERIOR_DELTOID,

    // Arms
    BICEPS,
    TRICEPS,
    FOREARM;

    public fun color(preset: MuscleColorPreset): Color {
        return when (this) {
            // Chest
            PECTORALIS_MAJOR_CLAVICULAR -> preset.pectoralisMajorClavicular
            PECTORALIS_MAJOR_STERNOCOSTAL -> preset.pectoralisMajorSternocostal
            PECTORALIS_MAJOR_ABDOMINAL -> preset.pectoralisMajorAbdominal

            // Back
            TRAPEZIUS -> preset.trapezius
            LATISSIMUS_DORSI -> preset.latissimus
            RHOMBOIDS -> preset.rhomboids
            TERES_MAJOR -> preset.teresMajor

            // Abdominal
            RECTUS_ABDOMINIS -> preset.rectusAbdominis
            OBLIQUES -> preset.obliquesAbdominis

            // Legs
            CALF -> preset.calf
            GLUTEAL -> preset.gluteal
            HAMSTRINGS -> preset.hamstrings
            QUADRICEPS -> preset.quadriceps
            ADDUCTORS -> preset.adductors
            ABDUCTORS -> preset.abductors

            // Shoulder
            ANTERIOR_DELTOID -> preset.anteriorDeltoid
            LATERAL_DELTOID -> preset.lateralDeltoid
            POSTERIOR_DELTOID -> preset.posteriorDeltoid

            // Arms
            BICEPS -> preset.biceps
            TRICEPS -> preset.triceps
            FOREARM -> preset.forearm
        }
    }

    public companion object {
        public fun front(): List<MuscleEnumState> = listOf(
            PECTORALIS_MAJOR_CLAVICULAR,
            PECTORALIS_MAJOR_STERNOCOSTAL,
            PECTORALIS_MAJOR_ABDOMINAL,
            RECTUS_ABDOMINIS,
            OBLIQUES,
            ANTERIOR_DELTOID,
            LATERAL_DELTOID,
            QUADRICEPS,
            BICEPS,
            FOREARM
        )

        public fun back(): List<MuscleEnumState> = listOf(
            TRAPEZIUS,
            LATISSIMUS_DORSI,
            RHOMBOIDS,
            TERES_MAJOR,
            POSTERIOR_DELTOID,
            GLUTEAL,
            HAMSTRINGS,
            CALF,
            ADDUCTORS,
            ABDUCTORS,
            TRICEPS,
            FOREARM
        )
    }
}