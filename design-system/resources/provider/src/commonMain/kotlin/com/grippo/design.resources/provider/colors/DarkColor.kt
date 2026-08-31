package com.grippo.design.resources.provider.colors

import com.grippo.design.resources.provider.AppColor

public object DarkColor : AppColor {

    override val background: AppColor.BackgroundColors = object : AppColor.BackgroundColors {
        override val screen = AppPalette.NeutralDark.N100
        override val dialog = AppPalette.NeutralDark.N100
    }
}
