package com.grippo.state.muscles

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_abductors
import com.grippo.design.resources.provider.muscle_adductors
import com.grippo.design.resources.provider.muscle_anterior_deltoid
import com.grippo.design.resources.provider.muscle_biceps
import com.grippo.design.resources.provider.muscle_calf
import com.grippo.design.resources.provider.muscle_forearm
import com.grippo.design.resources.provider.muscle_gluteal
import com.grippo.design.resources.provider.muscle_hamstrings
import com.grippo.design.resources.provider.muscle_lateral_deltoid
import com.grippo.design.resources.provider.muscle_latissimus_dorsi
import com.grippo.design.resources.provider.muscle_obliques
import com.grippo.design.resources.provider.muscle_pectoralis_major_abdominal
import com.grippo.design.resources.provider.muscle_pectoralis_major_clavicular
import com.grippo.design.resources.provider.muscle_pectoralis_major_sternocostal
import com.grippo.design.resources.provider.muscle_posterior_deltoid
import com.grippo.design.resources.provider.muscle_quadriceps
import com.grippo.design.resources.provider.muscle_rectus_abdominis
import com.grippo.design.resources.provider.muscle_rhomboids
import com.grippo.design.resources.provider.muscle_teres_major
import com.grippo.design.resources.provider.muscle_trapezius
import com.grippo.design.resources.provider.muscle_triceps
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.state.formatters.UiText

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

    public fun title(): UiText {
        val r = when (this) {
            PECTORALIS_MAJOR_CLAVICULAR -> Res.string.muscle_pectoralis_major_clavicular
            PECTORALIS_MAJOR_STERNOCOSTAL -> Res.string.muscle_pectoralis_major_sternocostal
            PECTORALIS_MAJOR_ABDOMINAL -> Res.string.muscle_pectoralis_major_abdominal
            TRAPEZIUS -> Res.string.muscle_trapezius
            LATISSIMUS_DORSI -> Res.string.muscle_latissimus_dorsi
            RHOMBOIDS -> Res.string.muscle_rhomboids
            TERES_MAJOR -> Res.string.muscle_teres_major
            RECTUS_ABDOMINIS -> Res.string.muscle_rectus_abdominis
            OBLIQUES -> Res.string.muscle_obliques
            CALF -> Res.string.muscle_calf
            GLUTEAL -> Res.string.muscle_gluteal
            HAMSTRINGS -> Res.string.muscle_hamstrings
            QUADRICEPS -> Res.string.muscle_quadriceps
            ADDUCTORS -> Res.string.muscle_adductors
            ABDUCTORS -> Res.string.muscle_abductors
            ANTERIOR_DELTOID -> Res.string.muscle_anterior_deltoid
            LATERAL_DELTOID -> Res.string.muscle_lateral_deltoid
            POSTERIOR_DELTOID -> Res.string.muscle_posterior_deltoid
            BICEPS -> Res.string.muscle_biceps
            TRICEPS -> Res.string.muscle_triceps
            FOREARM -> Res.string.muscle_forearm
        }
        return UiText.Res(r)
    }
}