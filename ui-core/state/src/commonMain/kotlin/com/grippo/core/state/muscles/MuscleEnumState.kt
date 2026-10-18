package com.grippo.core.state.muscles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
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
import com.grippo.design.resources.provider.muscles.bodyBack
import com.grippo.design.resources.provider.muscles.bodyFront
import com.grippo.design.resources.provider.muscles.bodySplit
import com.grippo.design.resources.provider.muscles.legsSplit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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

    public fun group(): MuscleGroupEnumState = when (this) {
        PECTORALIS_MAJOR_CLAVICULAR,
        PECTORALIS_MAJOR_STERNOCOSTAL,
        PECTORALIS_MAJOR_ABDOMINAL -> MuscleGroupEnumState.CHEST_MUSCLES

        TRAPEZIUS,
        LATISSIMUS_DORSI,
        RHOMBOIDS,
        TERES_MAJOR -> MuscleGroupEnumState.BACK_MUSCLES

        RECTUS_ABDOMINIS,
        OBLIQUES -> MuscleGroupEnumState.ABDOMINAL_MUSCLES

        CALF,
        GLUTEAL,
        HAMSTRINGS,
        QUADRICEPS,
        ADDUCTORS,
        ABDUCTORS -> MuscleGroupEnumState.LEGS

        ANTERIOR_DELTOID,
        LATERAL_DELTOID,
        POSTERIOR_DELTOID -> MuscleGroupEnumState.SHOULDER_MUSCLES

        BICEPS,
        TRICEPS,
        FOREARM -> MuscleGroupEnumState.ARMS_AND_FOREARMS
    }

    @Composable
    public fun color(): Color = group().color()

    @Composable
    public fun icon(color: Color = color()): ImageVector {
        val colors = AppTokens.colors.muscle
        val identity = color
        val preset = remember(this, colors, identity) {
            colorPreset(
                fallback = colors.inactive,
                outline = colors.outline,
                background = colors.background,
                colorFor = { muscle -> if (muscle == this) identity else colors.inactive },
            )
        }
        return image(preset)
    }

    @Composable
    public fun image(preset: MuscleColorPreset): ImageVector = remember(this, preset) {
        when (this) {
            PECTORALIS_MAJOR_CLAVICULAR,
            PECTORALIS_MAJOR_STERNOCOSTAL,
            PECTORALIS_MAJOR_ABDOMINAL,
            RECTUS_ABDOMINIS,
            OBLIQUES,
            ANTERIOR_DELTOID,
            BICEPS -> bodyFront(preset)

            TRAPEZIUS,
            LATISSIMUS_DORSI,
            RHOMBOIDS,
            TERES_MAJOR,
            POSTERIOR_DELTOID,
            TRICEPS -> bodyBack(preset)

            LATERAL_DELTOID,
            FOREARM -> bodySplit(preset)

            CALF,
            GLUTEAL,
            HAMSTRINGS,
            QUADRICEPS,
            ADDUCTORS,
            ABDUCTORS -> legsSplit(preset)
        }
    }

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

    public fun title(): UiText = TITLES.getValue(this)

    public companion object {
        public fun front(): ImmutableList<MuscleEnumState> = FRONT
        public fun back(): ImmutableList<MuscleEnumState> = BACK

        public fun colorPreset(
            fallback: Color,
            outline: Color,
            background: Color,
            colorFor: (MuscleEnumState) -> Color,
        ): MuscleColorPreset = MuscleColorPreset(
            biceps = colorFor(BICEPS),
            triceps = colorFor(TRICEPS),
            forearm = colorFor(FOREARM),
            forearmFront = colorFor(FOREARM),
            forearmBack = colorFor(FOREARM),
            lateralDeltoid = colorFor(LATERAL_DELTOID),
            anteriorDeltoid = colorFor(ANTERIOR_DELTOID),
            posteriorDeltoid = colorFor(POSTERIOR_DELTOID),
            pectoralisMajorAbdominal = colorFor(PECTORALIS_MAJOR_ABDOMINAL),
            pectoralisMajorClavicular = colorFor(PECTORALIS_MAJOR_CLAVICULAR),
            pectoralisMajorSternocostal = colorFor(PECTORALIS_MAJOR_STERNOCOSTAL),
            rectusAbdominis = colorFor(RECTUS_ABDOMINIS),
            obliquesAbdominis = colorFor(OBLIQUES),
            rhomboids = colorFor(RHOMBOIDS),
            latissimus = colorFor(LATISSIMUS_DORSI),
            trapezius = colorFor(TRAPEZIUS),
            teresMajor = colorFor(TERES_MAJOR),
            gluteal = colorFor(GLUTEAL),
            hamstrings = colorFor(HAMSTRINGS),
            calf = colorFor(CALF),
            quadriceps = colorFor(QUADRICEPS),
            adductors = colorFor(ADDUCTORS),
            abductors = colorFor(ABDUCTORS),
            other = fallback,
            outline = outline,
            backgroundFront = background,
            backgroundBack = background,
        )

        private val FRONT: ImmutableList<MuscleEnumState> = persistentListOf(
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

        private val BACK: ImmutableList<MuscleEnumState> = persistentListOf(
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

        private val TITLES: Map<MuscleEnumState, UiText> = entries.associateWith {
            UiText.Res(
                when (it) {
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
            )
        }
    }
}
