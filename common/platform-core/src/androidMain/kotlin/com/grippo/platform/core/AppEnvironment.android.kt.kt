package com.grippo.platform.core

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

public actual object LocalAppLocale {
    private var default: Locale? = null

    public actual val current: String
        @Composable get() = Locale.getDefault().toString()

    public actual fun system(): String = Locale.getDefault().toString()

    @Composable
    public actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = Configuration(LocalConfiguration.current)
        val resources = LocalContext.current.resources

        if (default == null) default = Locale.getDefault()

        val newLocale = when (value) {
            null -> default!!
            else -> Locale(value)
        }

        Locale.setDefault(newLocale)
        configuration.setLocale(newLocale)

        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)

        return LocalConfiguration.provides(configuration)
    }
}

public actual object LocalAppTheme {
    public actual val current: Boolean
        @Composable get() = (LocalConfiguration.current.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES

    public actual fun isSystemDark(): Boolean {
        val uiMode = Resources.getSystem().configuration.uiMode
        return (uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
    }

    @Composable
    public actual infix fun provides(value: Boolean?): ProvidedValue<*> {
        val base = LocalConfiguration.current
        val newCfg = if (value == null) {
            base
        } else {
            Configuration(base).apply {
                uiMode = when (value) {
                    true -> (uiMode and UI_MODE_NIGHT_MASK.inv()) or UI_MODE_NIGHT_YES
                    false -> (uiMode and UI_MODE_NIGHT_MASK.inv()) or UI_MODE_NIGHT_NO
                }
            }
        }
        return LocalConfiguration.provides(newCfg)
    }
}