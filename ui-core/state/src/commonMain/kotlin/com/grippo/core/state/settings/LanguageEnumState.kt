package com.grippo.core.state.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.language_english
import com.grippo.design.resources.provider.language_portuguese_brazil
import com.grippo.design.resources.provider.language_portuguese_portugal
import com.grippo.design.resources.provider.language_russian
import com.grippo.design.resources.provider.language_ukrainian

@Immutable
public enum class LanguageEnumState {
    ENGLISH,
    UKRAINIAN,
    RUSSIAN,
    PORTUGUESE_BRAZIL,
    PORTUGUESE_PORTUGAL;

    @Composable
    public fun title(): String {
        return when (this) {
            ENGLISH -> AppTokens.strings.res(Res.string.language_english)
            UKRAINIAN -> AppTokens.strings.res(Res.string.language_ukrainian)
            RUSSIAN -> AppTokens.strings.res(Res.string.language_russian)
            PORTUGUESE_BRAZIL -> AppTokens.strings.res(Res.string.language_portuguese_brazil)
            PORTUGUESE_PORTUGAL -> AppTokens.strings.res(Res.string.language_portuguese_portugal)
        }
    }

    public companion object {
        @Composable
        public fun current(): LanguageEnumState {
            return of(AppTokens.strings.localeTag)
        }

        public fun of(tag: String): LanguageEnumState {
            val subtags = tag.replace('_', '-').split('-')
            val language = subtags.first().lowercase()
            val region = subtags.drop(1).firstOrNull { it.length == 2 }?.uppercase()

            return when (language) {
                "uk" -> UKRAINIAN
                "ru" -> RUSSIAN
                "pt" -> if (region == "PT") PORTUGUESE_PORTUGAL else PORTUGUESE_BRAZIL
                else -> ENGLISH
            }
        }
    }
}
