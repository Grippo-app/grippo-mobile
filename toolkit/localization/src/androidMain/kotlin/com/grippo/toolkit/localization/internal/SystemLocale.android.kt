package com.grippo.toolkit.localization.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale
import kotlin.concurrent.Volatile

internal actual object SystemLocale {

    @Volatile
    private var initialDefault: Locale? = null

    actual val current: String
        @Composable get() {
            val configuration = LocalConfiguration.current
            val locale = configuration.locales[0] ?: Locale.getDefault()
            return locale.toLanguageTag()
        }

    actual fun current(): String {
        return Locale.getDefault().toLanguageTag()
    }

    actual fun apply(tag: String?) {
        when (tag) {
            null -> restoreDefault()
            else -> overrideDefault(tag)
        }
    }

    private fun restoreDefault() {
        val locale = initialDefault ?: return
        Locale.setDefault(locale)
    }

    private fun overrideDefault(tag: String) {
        if (initialDefault == null) {
            initialDefault = Locale.getDefault()
        }

        Locale.setDefault(Locale.forLanguageTag(tag))
    }
}
