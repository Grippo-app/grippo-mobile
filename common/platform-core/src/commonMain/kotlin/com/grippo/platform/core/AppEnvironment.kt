package com.grippo.platform.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection

// ---------- Locale ----------
private var customAppLocale: String? by mutableStateOf(null)

public expect object LocalAppLocale {
    public val current: String
        @Composable get

    public fun system(): String

    @Composable
    public infix fun provides(value: String?): ProvidedValue<*>
}

// Optional: derive RTL/LTR from locale tag (BCP-47)
public fun layoutDirectionFor(tag: String?): LayoutDirection {
    val rtl = setOf("ar", "fa", "he", "ur", "ps", "sd")
    val lang = tag?.substringBefore('-')?.lowercase()
    return if (lang in rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
}

// ---------- Theme ----------
private var customAppThemeIsDark: Boolean? by mutableStateOf(null)

public expect object LocalAppTheme {
    public val current: Boolean
        @Composable get

    public fun isSystemDark(): Boolean

    @Composable
    public infix fun provides(value: Boolean?): ProvidedValue<*>
}