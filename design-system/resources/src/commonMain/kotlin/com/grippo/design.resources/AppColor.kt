package com.grippo.design.resources

import androidx.compose.ui.graphics.Color

public interface AppColor {
    public val button: ButtonColors
    public val input: InputColors
    public val background: BackgroundColors
    public val dialog: DialogColors
    public val text: TextColors
    public val divider: DividerColors
    public val semantic: SemanticColors
    public val overlay: OverlayColors
    public val border: BorderColors
    public val interaction: InteractionColors
    public val elevation: ElevationColors
    public val skeleton: SkeletonColors
    public val icon: IconColors
    public val toggle: ToggleColors
    public val muscle: MuscleColors
    public val segment: SegmentColors
    public val konfetti: Konfetti
    public val muscleColorPreset: MuscleColorPreset

    public interface DividerColors {
        public val default: Color
    }

    public interface ButtonColors {
        public val backgroundPrimary: Color
        public val contentPrimary: Color
        public val backgroundPrimaryDisabled: Color
        public val contentPrimaryDisabled: Color

        public val backgroundSecondary: Color
        public val contentSecondary: Color
        public val backgroundSecondaryDisabled: Color
        public val contentSecondaryDisabled: Color

        public val contentTransparentDisabled: Color
    }

    public interface ToggleColors {
        public val checkedThumb: Color
        public val checkedTrack: Color

        public val uncheckedThumb: Color
        public val uncheckedTrack: Color

        public val disabledCheckedThumb: Color
        public val disabledCheckedTrack: Color

        public val disabledUncheckedThumb: Color
        public val disabledUncheckedTrack: Color
        public val disabledUncheckedBorder: Color
    }

    public interface InputColors {
        public val background: Color
        public val border: Color
        public val placeholder: Color
        public val label: Color
        public val text: Color

        public val leading: Color
        public val trailing: Color

        public val backgroundDisabled: Color
        public val borderDisabled: Color
        public val textDisabled: Color
        public val placeholderDisabled: Color
    }

    public interface BackgroundColors {
        public val primary: Color
        public val secondary: Color
        public val accent: Color
    }

    public interface DialogColors {
        public val background: Color
        public val handle: Color
        public val scrim: Color
    }

    public interface TextColors {
        public val primary: Color
        public val secondary: Color
        public val disabled: Color
    }

    public interface SemanticColors {
        public val success: Color
        public val error: Color
        public val warning: Color
        public val info: Color
    }

    public interface OverlayColors {
        public val defaultShadow: Color
        public val accentShadow: Color
    }

    public interface BorderColors {
        public val defaultPrimary: Color
        public val disabledPrimary: Color
        public val defaultSecondary: Color
        public val disabledSecondary: Color
        public val focus: Color
        public val error: Color
    }

    public interface InteractionColors {
        public val pressed: Color
        public val hovered: Color
        public val focused: Color
    }

    public interface ElevationColors {
        public val level0: Color
        public val level1: Color
        public val level2: Color
    }

    public interface SkeletonColors {
        public val background: Color
        public val shimmer: Color
    }

    public interface IconColors {
        public val default: Color
        public val disabled: Color
        public val accent: Color
        public val invert: Color
    }

    public interface EquipmentColors {
        public val inactive: Color
    }

    public interface MuscleColors {
        public val active: Color
        public val inactive: Color
        public val background: Color
        public val outline: Color
    }

    public interface SegmentColors {
        public val active: Color
        public val inactive: Color
        public val selector: Color
    }

    public interface Konfetti {
        public val confettiColor1: Color
        public val confettiColor2: Color
        public val confettiColor3: Color
        public val confettiColor4: Color
        public val confettiColor5: Color
        public val confettiColor6: Color
        public val confettiColor7: Color
        public val confettiColor8: Color
        public val confettiColor9: Color
        public val confettiColor10: Color
    }

    public interface MuscleColorPreset {
        public val pectoralisMajorClavicular: Color
        public val pectoralisMajorSternocostal: Color
        public val pectoralisMajorAbdominal: Color

        public val trapezius: Color
        public val latissimusDorsi: Color
        public val rhomboids: Color
        public val teresMajor: Color

        public val rectusAbdominis: Color
        public val obliques: Color

        public val calf: Color
        public val gluteal: Color
        public val hamstrings: Color
        public val quadriceps: Color
        public val adductors: Color
        public val abductors: Color

        public val anteriorDeltoid: Color
        public val lateralDeltoid: Color
        public val posteriorDeltoid: Color

        public val biceps: Color
        public val triceps: Color
        public val forearm: Color
    }
}