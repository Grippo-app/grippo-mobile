package com.grippo.design.resources.provider.providers

import com.grippo.design.resources.provider.AppColor

public interface ColorProvider {
    public suspend fun get(): AppColor
}