package com.grippo.design.resources.provider.impl.internal

import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.colors.LightColor
import com.grippo.design.resources.provider.providers.ColorProvider
import org.koin.core.annotation.Single

@Single(binds = [ColorProvider::class])
internal class ColorProviderImpl() : ColorProvider {
    override suspend fun get(): AppColor {
        return LightColor // TODO Implement
//        val palette = AppTokens.colors.charts.categorical.palette
//        if (palette.isEmpty()) return Color.Unspecified
//        return palette[(index % palette.size + palette.size) % palette.size]
    }
}
