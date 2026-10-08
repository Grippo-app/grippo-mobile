package com.grippo.shared.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.toolbar.BottomSheetToolbar
import com.grippo.design.core.AppTokens
import com.grippo.shared.dialog.content.DialogContentComponent
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun DialogScreen(
    component: DialogComponent,
    state: DialogState,
    loaders: ImmutableSet<DialogLoader>,
    contract: DialogContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    val slotState by component.childSlot.subscribeAsState()
    val child = slotState.child ?: return@BaseComposeScreen

    key(child.configuration.id) {
        BottomSheet(
            phase = state.phase,
            component = child.instance,
            onBack = { contract.onDismiss(null) },
            onClose = contract::onClose,
            onReleased = { contract.onRelease(child.configuration) }
        )
    }
}

@Composable
private fun BottomSheet(
    phase: SheetPhase,
    component: DialogContentComponent,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onReleased: () -> Unit
) {
    val onReleasedRef = rememberUpdatedState(onReleased)
    val contentStack by component.childStack.subscribeAsState()
    val activeConfig = contentStack.active.configuration.config
    val isSwipeRef = rememberUpdatedState(activeConfig.dismissBySwipe)
    val showBackButton = contentStack.backStack.isNotEmpty()
    val programmaticDismiss = phase == SheetPhase.Dismissing

    val programmaticDismissRef = rememberUpdatedState(programmaticDismiss)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            // Use the latest flag; do not rely on a stale capture
            if (target == SheetValue.Hidden && !isSwipeRef.value && !programmaticDismissRef.value) {
                return@rememberModalBottomSheetState false
            }
            true
        },
    )

    LaunchedEffect(phase) {
        if (programmaticDismiss) {
            sheetState.hide()
            onReleasedRef.value()
        }
    }

    ModalBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        onDismissRequest = { if (isSwipeRef.value) onReleasedRef.value() }, // latest flag
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets() },
        scrimColor = AppTokens.colors.dialog.scrim,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false, // Back is routed through the inner stack below.
        ),
        containerColor = AppTokens.colors.background.dialog,
        contentColor = AppTokens.colors.text.primary,
        dragHandle = null,
        shape = RoundedCornerShape(
            topStart = AppTokens.dp.bottomSheet.radius,
            topEnd = AppTokens.dp.bottomSheet.radius
        ),
    ) {
        BackHandler(enabled = phase == SheetPhase.Present, onBack = onBack)

        BottomSheetToolbar(
            modifier = Modifier.fillMaxWidth(),
            title = activeConfig.title.text(),
            onBack = onBack,
            onClose = onClose,
            allowBack = showBackButton
        )

        Column(
            modifier = Modifier.weight(weight = 1f, fill = false),
            content = { component.Render() }
        )
    }
}
