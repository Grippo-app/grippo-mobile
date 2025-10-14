package com.grippo.design.resources.provider.impl.internal

import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.colors.DarkColor
import com.grippo.design.resources.provider.colors.LightColor
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.toolkit.theme.LocalAppTheme
import org.koin.core.annotation.Single

@Single(binds = [ColorProvider::class])
internal class ColorProviderImpl() : ColorProvider {
    override suspend fun get(): AppColor {
        return if (LocalAppTheme.current()) {
            DarkColor
        } else {
            LightColor
        }
    }
}
