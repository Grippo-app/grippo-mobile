package com.grippo.design.components.utils

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberClipboardCopyAction(): (String) -> Unit
