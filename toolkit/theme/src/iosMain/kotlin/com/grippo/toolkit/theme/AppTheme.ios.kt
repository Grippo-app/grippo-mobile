package com.grippo.toolkit.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

public actual object AppTheme {
    public actual val current: Boolean
        @Composable get() = isSystemInDarkTheme()

    public actual fun current(): Boolean {
        val style = UIScreen.mainScreen.traitCollection.userInterfaceStyle
        return style == UIUserInterfaceStyle.UIUserInterfaceStyleDark
    }
}
