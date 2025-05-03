package com.grippo.design.resources

import androidx.compose.ui.graphics.Color

public interface AppColor {
    public val button: ButtonColors
    public val input: InputColors
    public val state: StateColors
    public val background: BackgroundColors
    public val bottomSheet: BottomSheetColors
    public val text: TextColors
    public val divider: Color

    public val semantic: SemanticColors
    public val overlay: OverlayColors
    public val border: BorderColors
    public val interaction: InteractionColors
    public val elevation: ElevationColors
    public val skeleton: SkeletonColors

    public interface ButtonColors {
        public val primaryBackground: Color
        public val primaryContent: Color
        public val primaryDisabledBackground: Color
        public val primaryDisabledContent: Color

        public val secondaryBackground: Color
        public val secondaryContent: Color
        public val secondaryDisabledBackground: Color
        public val secondaryDisabledContent: Color

        public val tertiaryBackground: Color
        public val tertiaryContent: Color
        public val tertiaryDisabledContent: Color
    }

    public interface InputColors {
        public val background: Color
        public val border: Color
        public val placeholder: Color
        public val text: Color

        public val disabledBackground: Color
        public val disabledBorder: Color
        public val disabledText: Color
        public val disabledPlaceholder: Color
    }

    public interface StateColors {
        public val error: Color
        public val success: Color
        public val warning: Color

        public val onError: Color
        public val onSuccess: Color
        public val onWarning: Color
    }

    public interface BackgroundColors {
        public val primary: Color
        public val secondary: Color
    }

    public interface BottomSheetColors {
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
}