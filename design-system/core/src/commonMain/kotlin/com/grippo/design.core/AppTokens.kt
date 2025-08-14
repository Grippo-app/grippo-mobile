package com.grippo.design.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import com.grippo.design.core.internal.LocalAppColors
import com.grippo.design.core.internal.LocalAppDp
import com.grippo.design.core.internal.LocalAppDrawables
import com.grippo.design.core.internal.LocalAppIcons
import com.grippo.design.core.internal.LocalAppStrings
import com.grippo.design.core.internal.LocalAppTypography
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.AppDp
import com.grippo.design.resources.provider.AppDrawable
import com.grippo.design.resources.provider.AppIcon
import com.grippo.design.resources.provider.AppString
import com.grippo.design.resources.provider.AppTypography

@Stable
public object AppTokens {
    public val colors: AppColor
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    public val icons: AppIcon
        @Composable
        @ReadOnlyComposable
        get() = LocalAppIcons.current

    public val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    public val strings: AppString
        @Composable
        @ReadOnlyComposable
        get() = LocalAppStrings.current

    public val drawables: AppDrawable
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDrawables.current

    public val dp: AppDp
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDp.current
}