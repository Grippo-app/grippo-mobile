package com.grippo.design.components.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIPasteboard

@Composable
internal actual fun rememberClipboardCopyAction(): (String) -> Unit = remember {
    { value -> UIPasteboard.generalPasteboard.string = value }
}
