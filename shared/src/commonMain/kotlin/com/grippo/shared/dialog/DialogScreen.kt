package com.grippo.shared.dialog

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import com.grippo.dialog.api.DialogConfig
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun DialogScreen(
    slot: Value<ChildSlot<DialogConfig, DialogComponent.Dialog>>,
    state: DialogState,
    loaders: ImmutableSet<DialogLoader>,
    contract: DialogContract
) {
    val state = slot.subscribeAsState()
    state.value.child?.instance?.component?.Render()
}