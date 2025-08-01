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
    public val skeleton: SkeletonColors
    public val icon: IconColors
    public val toggle: ToggleColors
    public val muscle: MuscleColors
    public val segment: SegmentColors
    public val konfetti: Konfetti
    public val chip: ChipColors
    public val theme: ThemeColors
    public val radio: RadioColors

    public interface DividerColors {
        public val primary: Color
        public val secondary: Color
    }

    public interface ButtonColors {
        public val backgroundPrimary: Color
        public val textPrimary: Color
        public val iconPrimary: Color
        public val backgroundPrimaryDisabled: Color
        public val contentPrimaryDisabled: Color

        public val backgroundSecondary: Color
        public val textSecondary: Color
        public val iconSecondary: Color
        public val borderSecondary: Color
        public val backgroundSecondaryDisabled: Color
        public val contentSecondaryDisabled: Color

        public val contentTransparentDisabled: Color
        public val textTransparent: Color
        public val iconTransparent: Color

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

    public interface RadioColors {
        public val selectedThumb: Color
        public val selectedTrack: Color

        public val unselectedThumb: Color
        public val unselectedTrack: Color

        public val disabledSelectedThumb: Color
        public val disabledSelectedTrack: Color

        public val disabledUnselectedThumb: Color
        public val disabledUnselectedTrack: Color
        public val disabledUnselectedBorder: Color
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
        public val tertiary: Color
    }

    public interface DialogColors {
        public val background: Color
        public val handle: Color
        public val scrim: Color
    }

    public interface TextColors {
        public val primary: Color
        public val secondary: Color
        public val tertiary: Color
        public val inverted: Color
        public val disabled: Color
    }

    public interface SemanticColors {
        public val success: Color
        public val error: Color
        public val warning: Color
        public val info: Color
        public val accent: Color
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
        public val inverted: Color
        public val focus: Color
        public val error: Color
    }

    public interface SkeletonColors {
        public val background: Color
        public val shimmer: Color
    }

    public interface IconColors {
        public val primary: Color
        public val secondary: Color
        public val disabled: Color
        public val accent: Color
        public val inverted: Color
    }

    public interface MuscleColors {
        public val focused: Color
        public val active: Color
        public val inactive: Color
        public val background: Color
        public val outline: Color
        public val palette: List<Color>
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

    public interface ChipColors {
        public val intensity: GradientColors
        public val tonnage: GradientColors
        public val repetitions: GradientColors

        public interface GradientColors {
            public val startColor: Color
            public val endColor: Color
            public val contentColor: Color
            public val borderColor: Color
        }
    }

    public interface ThemeColors {
        public val lightText: Color
        public val lightBackground1: Color
        public val lightBackground2: Color
        public val darkText: Color
        public val darkBackground1: Color
        public val darkBackground2: Color
    }
}
