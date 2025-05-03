package com.grippo.design.core

import androidx.compose.runtime.Composable
import com.grippo.design.core.internal.LocalAppColors
import com.grippo.design.core.internal.LocalAppIcons
import com.grippo.design.core.internal.LocalAppStrings
import com.grippo.design.core.internal.LocalAppTypography
import com.grippo.design.resources.AppColor
import com.grippo.design.resources.AppIcon
import com.grippo.design.resources.AppString
import com.grippo.design.resources.AppTypography

public object AppTokens {
    public val colors: AppColor
        @Composable
        get() = LocalAppColors.current

    public val icons: AppIcon
        @Composable
        get() = LocalAppIcons.current

    public val typography: AppTypography
        @Composable
        get() = LocalAppTypography.current

    public val strings: AppString
        @Composable
        get() = LocalAppStrings.current
}