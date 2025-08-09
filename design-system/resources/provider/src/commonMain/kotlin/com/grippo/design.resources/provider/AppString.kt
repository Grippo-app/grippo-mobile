package com.grippo.design.resources.provider

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

public object AppString {

    @Composable
    public fun res(value: StringResource, vararg formatArgs: Any): String {
        return stringResource(value, *formatArgs)
    }
}