package com.grippo.platform.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

@OptIn(InternalComposeUiApi::class)
public actual object LocalAppLocale {
    private const val LANG_KEY = "AppleLanguages"
    private val default: String = (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "en"
    private val LocalLocale = staticCompositionLocalOf { default }

    public actual val current: String
        @Composable get() = LocalLocale.current

    public actual fun system(): String = default

    @Composable
    public actual infix fun provides(value: String?): ProvidedValue<*> {
        val new = value ?: default
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(LANG_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(arrayListOf(new), LANG_KEY)
        }
        return LocalLocale.provides(new)
    }
}

@OptIn(InternalComposeUiApi::class)
public actual object LocalAppTheme {
    public actual val current: Boolean
        @Composable get() = LocalSystemTheme.current == SystemTheme.Dark

    public actual fun isSystemDark(): Boolean {
        val style = UIScreen.mainScreen.traitCollection.userInterfaceStyle
        return style == UIUserInterfaceStyle.UIUserInterfaceStyleDark
    }

    @Composable
    public actual infix fun provides(value: Boolean?): ProvidedValue<*> {
        val new = when (value) {
            true -> SystemTheme.Dark
            false -> SystemTheme.Light
            null -> LocalSystemTheme.current
        }
        return LocalSystemTheme.provides(new)
    }
}