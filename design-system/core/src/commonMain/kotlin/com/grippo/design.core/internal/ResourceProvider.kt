package com.grippo.design.core.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.grippo.design.resources.AppColor
import com.grippo.design.resources.AppDp
import com.grippo.design.resources.AppDrawable
import com.grippo.design.resources.AppIcon
import com.grippo.design.resources.AppString
import com.grippo.design.resources.AppTypography
import com.grippo.design.resources.colors.DarkColor
import com.grippo.design.resources.colors.LightColor

@Composable
internal fun ProvideResources(
    darkTheme: Boolean,
    vararg values: ProvidedValue<*>,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppColors provides if (darkTheme) DarkColor else LightColor,
        LocalAppIcons provides AppIcon,
        LocalAppTypography provides AppTypography,
        LocalAppStrings provides AppString,
        LocalAppDrawables provides AppDrawable,
        LocalAppDp provides AppDp,
        *values,
        content = content,
    )
}

internal val LocalAppColors = staticCompositionLocalOf<AppColor> {
    error("No colors provided")
}
internal val LocalAppIcons = staticCompositionLocalOf<AppIcon> {
    error("No icons provided")
}
internal val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("No typography provided")
}
internal val LocalAppStrings = staticCompositionLocalOf<AppString> {
    error("No strings provided")
}
internal val LocalAppDrawables = staticCompositionLocalOf<AppDrawable> {
    error("No drawables provided")
}
internal val LocalAppDp = staticCompositionLocalOf<AppDp> {
    error("No dp provided")
}