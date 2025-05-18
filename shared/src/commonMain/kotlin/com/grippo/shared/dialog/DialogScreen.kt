package com.grippo.shared.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import com.grippo.core.BaseComposeScreen
import com.grippo.design.core.AppTokens
import com.grippo.dialog.api.DialogConfig
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun DialogScreen(
    slot: Value<ChildSlot<DialogConfig, DialogComponent.Dialog>>,
    state: DialogState,
    loaders: ImmutableSet<DialogLoader>,
    contract: DialogContract
) = BaseComposeScreen {
    val slotState = slot.subscribeAsState()

    val child = slotState.value.child
    val component = child?.instance?.component

    if (component != null) {
        val config = child.configuration

        val modalBottomSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { true },
        )

        LaunchedEffect(state.process) {
            if (state.process == Process.DISMISS) {
                modalBottomSheetState.hide()
                contract.release(config)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { contract.release(config) },
            sheetState = modalBottomSheetState,
            scrimColor = AppTokens.colors.dialog.scrim,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
            containerColor = AppTokens.colors.dialog.background,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = AppTokens.colors.dialog.handle
                )
            },
            content = {
                Column(
                    modifier = Modifier.navigationBarsPadding(),
                    content = { component.Render() }
                )
            },
        )
    }
}