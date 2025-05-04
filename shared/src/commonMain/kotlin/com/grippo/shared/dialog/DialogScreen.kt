package com.grippo.shared.dialog

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogEvent
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun DialogScreen(
    slot: Value<ChildSlot<DialogConfig, DialogComponent.Child>>,
    dialog: SlotNavigation<DialogConfig>,

    state: DialogState,
    loaders: ImmutableSet<DialogLoader>,
    contract: DialogContract
) {

    LaunchedEffect(state.event) {
        when (state.event) {
            DialogEvent.Dismiss -> dialog.dismiss()
            is DialogEvent.Show -> dialog.activate(state.event.config)
            null -> return@LaunchedEffect
        }
    }

    val child = slot.value.child

    if (child != null) {
        println("HELLO WORLD CHILD = $child")
        ModalBottomSheet(
            onDismissRequest = contract::dismiss,
            content = { child.instance.component.Render() }
        )
    }
}