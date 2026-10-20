package com.grippo.toolkit.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

public actual object AppLocale {

    public actual val current: String
        @Composable get() {
            val conf = LocalConfiguration.current
            return effectiveLocale(conf.locales[0] ?: Locale.getDefault()).toLanguageTag()
        }

    public actual fun current(): String = effectiveLocale(Locale.getDefault()).toLanguageTag()
}

private fun effectiveLocale(fallback: Locale): Locale =
    AppCompatDelegate.getApplicationLocales()[0] ?: fallback
