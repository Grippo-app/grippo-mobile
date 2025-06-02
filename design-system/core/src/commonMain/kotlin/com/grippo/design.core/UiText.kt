package com.grippo.design.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource

@Stable
public sealed interface UiText {
    @Immutable
    public data class Res(val value: StringResource, val formatArgs: List<Any> = emptyList()) :
        UiText

    @Immutable
    public data class Str(val value: String) : UiText

    @Composable
    public fun text(): String {
        return when (this) {
            is Str -> value
            is Res -> AppTokens.strings.res(value, *formatArgs.toTypedArray())
        }
    }
}