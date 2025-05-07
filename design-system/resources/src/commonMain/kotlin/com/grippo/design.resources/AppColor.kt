package com.grippo.design.resources

import androidx.compose.ui.graphics.Color

public interface AppColor {
    public val button: ButtonColors
    public val input: InputColors
    public val background: BackgroundColors
    public val dialog: DialogColors
    public val text: TextColors
    public val divider: Color
    public val semantic: SemanticColors
    public val overlay: OverlayColors
    public val border: BorderColors
    public val interaction: InteractionColors
    public val elevation: ElevationColors
    public val skeleton: SkeletonColors
    public val icon: IconColors

    public interface ButtonColors {
        public val backgroundPrimary: Color
        public val contentPrimary: Color
        public val backgroundPrimaryDisabled: Color
        public val contentPrimaryDisabled: Color

        public val backgroundSecondary: Color
        public val contentSecondary: Color
        public val backgroundSecondaryDisabled: Color
        public val contentSecondaryDisabled: Color

        public val backgroundTertiary: Color
        public val contentTertiary: Color
        public val backgroundTertiaryDisabled: Color
        public val contentTertiaryDisabled: Color

        public val contentTransparentDisabled: Color
    }

    public interface InputColors {
        public val background: Color
        public val border: Color
        public val placeholder: Color
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
        public val level1: Color
        public val level2: Color
        public val level3: Color
        public val level4: Color
    }

    public interface BorderColors {
        public val default: Color
        public val focus: Color
        public val disabled: Color
        public val error: Color

        public val defaultSecondary: Color
        public val disabledSecondary: Color
        public val defaultTertiary: Color
        public val disabledTertiary: Color
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
    }
}