package com.grippo.design.resources.provider

import androidx.compose.ui.graphics.Color

public interface AppColor {
    public val background: BackgroundColors

    public interface BackgroundColors {
        public val screen: Color
        public val dialog: Color
    }
}
