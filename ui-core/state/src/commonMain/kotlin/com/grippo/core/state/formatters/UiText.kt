package com.grippo.core.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.providers.StringProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource

@Stable
public sealed interface UiText {
    @Immutable
    public data class Res(
        val value: StringResource,
        val formatArgs: ImmutableList<Any> = persistentListOf()
    ) :
        UiText

    @Immutable
    public data class Str(val value: String) : UiText

    @Composable
    public fun text(): String {
        return when (this) {
            is Str -> value
            is Res -> {
                val args = remember(formatArgs) { formatArgs.toTypedArray() }
                AppTokens.strings.res(value, *args)
            }
        }
    }

    public suspend fun text(stringProvider: StringProvider): String {
        return when (this) {
            is Str -> value
            is Res -> stringProvider.get(value, *formatArgs.toTypedArray())
        }
    }
}
