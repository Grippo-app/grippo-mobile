package com.grippo.design.resources.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Immutable
public data class AppString(public val localeTag: String) {

    @Composable
    public fun res(value: StringResource, vararg formatArgs: Any): String {
        return stringResource(value, *formatArgs)
    }

    @Composable
    public fun res(value: PluralStringResource, quantity: Int, vararg formatArgs: Any): String {
        return pluralStringResource(value, quantity, *formatArgs)
    }
}
