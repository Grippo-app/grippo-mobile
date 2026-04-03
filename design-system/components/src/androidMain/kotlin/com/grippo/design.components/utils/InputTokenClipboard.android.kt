package com.grippo.design.components.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberClipboardCopyAction(): (String) -> Unit {
    val context = LocalContext.current

    return remember(context) {
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        { value: String ->
            clipboardManager?.setPrimaryClip(ClipData.newPlainText(null, value))
        }
    }
}
