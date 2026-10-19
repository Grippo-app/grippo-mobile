package com.grippo.shared.dialog.content.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder

@Composable
internal fun RetainDialogContentStates(
    holder: SaveableStateHolder,
    currentKeys: Set<String>,
) {
    val retainedKeys = remember(holder) { RetainedDialogContentKeys(keys = currentKeys) }

    DisposableEffect(holder, currentKeys) {
        retainedKeys.keys.forEach {
            if (it !in currentKeys) {
                holder.removeState(it)
            }
        }

        retainedKeys.keys = currentKeys

        onDispose {}
    }
}

private class RetainedDialogContentKeys(
    var keys: Set<String>,
)
