package com.grippo.design.core.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.AppDp
import com.grippo.design.resources.provider.AppIcon
import com.grippo.design.resources.provider.AppString
import com.grippo.design.resources.provider.AppTypography
import com.grippo.design.resources.provider.colors.DarkColor

@Composable
internal fun ProvideResources(
    darkTheme: Boolean,
    localeTag: String,
    vararg values: ProvidedValue<*>,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppColors provides if (darkTheme) DarkColor else DarkColor,
        LocalAppIcons provides AppIcon,
        LocalAppTypography provides AppTypography,
        LocalAppStrings provides AppString,
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
internal val LocalAppDp = staticCompositionLocalOf<AppDp> {
    error("No dp provided")
}