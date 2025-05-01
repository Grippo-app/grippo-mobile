package com.grippo.design.core.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.grippo.design.core.models.ColorToken
import com.grippo.design.core.models.DarkColorToken
import com.grippo.design.core.models.IconToken
import com.grippo.design.core.models.LightColorToken

internal val LocalAppColors = staticCompositionLocalOf<ColorToken> { error("No colors provided") }
internal val LocalAppIcons = staticCompositionLocalOf<IconToken> { error("No icons provided") }

@Composable
internal fun ProvideResources(
    darkTheme: Boolean,
    vararg values: ProvidedValue<*>,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppColors provides if (darkTheme) DarkColorToken else LightColorToken,
        LocalAppIcons provides IconToken,
        *values,
        content = content,
    )
}