package com.grippo.design.core

import androidx.compose.runtime.Composable
import com.grippo.design.core.internal.LocalAppColors
import com.grippo.design.core.internal.LocalAppIcons
import com.grippo.design.core.models.ColorToken
import com.grippo.design.core.models.IconToken

public object AppTokens {
    public val colors: ColorToken
        @Composable
        get() = LocalAppColors.current

    public val icons: IconToken
        @Composable
        get() = LocalAppIcons.current
}