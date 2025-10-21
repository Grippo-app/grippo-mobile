package com.grippo.toolkit.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

@OptIn(InternalComposeUiApi::class)
public actual object AppTheme {
    public actual val current: Boolean
        @Composable get() = LocalSystemTheme.current == SystemTheme.Dark

    public actual fun current(): Boolean {
        val style = UIScreen.mainScreen.traitCollection.userInterfaceStyle
        return style == UIUserInterfaceStyle.UIUserInterfaceStyleDark
    }
}
