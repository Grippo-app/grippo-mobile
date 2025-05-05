package com.grippo.shared.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.dialog.api.DialogEvent
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
internal fun DialogScreen(
    slot: Value<ChildSlot<DialogConfig, DialogComponent.Dialog>>,
    dialog: SlotNavigation<DialogConfig>,
    dialogController: DialogController,

    state: DialogState,
    loaders: ImmutableSet<DialogLoader>,
    contract: DialogContract
) {
    LaunchedEffect(Unit) {
        dialogController.dialog.onEach {
            when (it) {
                DialogEvent.Dismiss -> dialog.dismiss()
                is DialogEvent.Show -> dialog.activate(it.config)
            }
        }.launchIn(this)
    }

    val state = slot.subscribeAsState()

    state.value.child?.instance?.component?.Render()
}