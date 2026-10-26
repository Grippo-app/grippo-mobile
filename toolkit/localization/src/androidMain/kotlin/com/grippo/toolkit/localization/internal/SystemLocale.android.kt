package com.grippo.toolkit.localization.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

internal actual object SystemLocale {

    actual val current: String
        @Composable get() {
            val configuration = LocalConfiguration.current
            val locale = configuration.locales[0] ?: Locale.getDefault()
            return locale.toLanguageTag()
        }

    actual fun current(): String {
        return Locale.getDefault().toLanguageTag()
    }

    actual fun apply(tag: String) {
        Locale.setDefault(Locale.forLanguageTag(tag))
    }
}
