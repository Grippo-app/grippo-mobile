package com.grippo.presentation.api.muscles.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.AppColor

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

    @Composable
    public fun color(): Color {
        val colors = AppTokens.colors.muscle.colorful
        return allocateColorByMuscle(colors)
    }

    public fun allocateColorByMuscle(colors: AppColor.MuscleColors.Colorful): Color {
        return when (this) {
            // Chest
            PECTORALIS_MAJOR_CLAVICULAR -> colors.pectoralisMajorClavicular
            PECTORALIS_MAJOR_STERNOCOSTAL -> colors.pectoralisMajorSternocostal
            PECTORALIS_MAJOR_ABDOMINAL -> colors.pectoralisMajorAbdominal

            // Back
            TRAPEZIUS -> colors.trapezius
            LATISSIMUS_DORSI -> colors.latissimusDorsi
            RHOMBOIDS -> colors.rhomboids
            TERES_MAJOR -> colors.teresMajor

            // Abdominal
            RECTUS_ABDOMINIS -> colors.rectusAbdominis
            OBLIQUES -> colors.obliques

            // Legs
            CALF -> colors.calf
            GLUTEAL -> colors.gluteal
            HAMSTRINGS -> colors.hamstrings
            QUADRICEPS -> colors.quadriceps
            ADDUCTORS -> colors.adductors
            ABDUCTORS -> colors.abductors

            // Shoulder
            ANTERIOR_DELTOID -> colors.anteriorDeltoid
            LATERAL_DELTOID -> colors.lateralDeltoid
            POSTERIOR_DELTOID -> colors.posteriorDeltoid

            // Arms
            BICEPS -> colors.biceps
            TRICEPS -> colors.triceps
            FOREARM -> colors.forearm
        }
    }
}