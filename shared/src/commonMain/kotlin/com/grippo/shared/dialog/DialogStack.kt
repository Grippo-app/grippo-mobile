package com.grippo.shared.dialog

import androidx.compose.runtime.Immutable
import com.grippo.dialog.api.DialogConfig

@Immutable
internal data class DialogStack(
    val stack: List<DialogConfig> = emptyList()
) {
    val current: DialogConfig? get() = stack.lastOrNull()
    fun push(config: DialogConfig) = DialogStack(stack + config)
    fun pop(): DialogStack = DialogStack(stack.dropLast(1))
}