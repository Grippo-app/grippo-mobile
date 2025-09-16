package com.grippo.platform.core

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

private val LocalLocaleOverride = staticCompositionLocalOf<String?> { null }

public actual object LocalAppLocale {

    public actual val current: String
        @Composable get() {
            // 1) If there is an override in Composition, return it.
            LocalLocaleOverride.current?.let { return normalizeTag(it) }

            // 2) Else respect per-app locales if set via AppCompatDelegate (pre-Android 13)
            val appTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (appTags.isNotBlank()) return appTags

            // 3) Else read from current configuration (triggers recomposition on config change)
            val conf = LocalConfiguration.current
            val confTags = conf.locales.toLanguageTags()
            if (confTags.isNotBlank()) return confTags

            // 4) Fallback to system default
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