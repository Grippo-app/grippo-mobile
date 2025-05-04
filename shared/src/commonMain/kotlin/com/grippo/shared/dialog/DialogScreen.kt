package com.grippo.shared.dialog

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import com.grippo.core.BaseComponent
import com.grippo.core.models.BaseDirection
import com.grippo.dialog.api.DialogConfig
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun DialogScreen(
    slot: Value<ChildSlot<DialogConfig, BaseComponent<out BaseDirection>>>,
    state: DialogState,
    loaders: ImmutableSet<DialogLoader>,
    contract: DialogContract
) {
    println("HELLO WORLD")
    println("HELLO WORLD")
    println("HELLO WORLD")
    val child = slot.value.child

    if (child != null) {
        ModalBottomSheet(
            onDismissRequest = contract::dismiss,
            content = { child.instance.Render() }
        )
    }
}