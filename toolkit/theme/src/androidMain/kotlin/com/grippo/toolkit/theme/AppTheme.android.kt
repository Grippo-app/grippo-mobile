package com.grippo.toolkit.theme

import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

public actual object AppTheme {
    public actual val current: Boolean
        @Composable get() = (LocalConfiguration.current.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES

    public actual fun current(): Boolean {
        val uiMode = Resources.getSystem().configuration.uiMode
        return (uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
    }
}
