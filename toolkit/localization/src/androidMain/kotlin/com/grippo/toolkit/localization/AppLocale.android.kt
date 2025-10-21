package com.grippo.toolkit.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

public actual object AppLocale {

    public actual val current: String
        @Composable get() {
            // Respect per-app locales if set via AppCompatDelegate (pre-Android 13)
            val appTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (appTags.isNotBlank()) return appTags

            // Else read from current configuration (triggers recomposition on config change)
            val conf = LocalConfiguration.current
            val confTags = conf.locales.toLanguageTags()
            if (confTags.isNotBlank()) return confTags

            // Fallback to system default
            return Locale.getDefault().toLanguageTag()
        }

    public actual fun current(): String = Locale.getDefault().toLanguageTag()
}

private fun normalizeTag(tag: String): String {
    // Ensure BCP-47 normalization like "uk-UA"
    return try {
        Locale.forLanguageTag(tag).toLanguageTag()
    } catch (_: Throwable) {
        Locale(tag).toLanguageTag()
    }
}
