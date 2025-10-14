package com.grippo.shared.dialog

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.toolbar.BottomSheetToolbar
import com.grippo.design.core.AppTokens
import com.grippo.dialog.api.DialogConfig
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

    val contentComponent = (child.instance.component as? DialogContentComponent)
        ?: return@BaseComposeScreen

    BottomSheet(
        config = child.configuration,
        stack = state.stack,
        phase = state.phase,
        component = contentComponent,
        onBack = { contract.onDismiss(null) },
        onClose = contract::onClose,
        onDismiss = { contract.onRelease(child.configuration) },
        onDismissComplete = { contract.onRelease(child.configuration) }
    )
}

@Composable
private fun BottomSheet(
    config: DialogConfig,
    stack: List<DialogEntry>,
    phase: SheetPhase,
    component: DialogContentComponent,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onDismiss: () -> Unit,
    onDismissComplete: () -> Unit
) {
    val onDismissCompleteRef = rememberUpdatedState(onDismissComplete)

    // Compute the current flag from the top entry
    val isSwipeDismissEnabled = stack.lastOrNull()?.config?.dismissBySwipe ?: true
    // Keep the latest value for lambdas captured once
    val isSwipeRef = rememberUpdatedState(isSwipeDismissEnabled)

    val showBackButton = stack.size > 1
    val programmaticDismiss = phase == SheetPhase.DISMISSING

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            // Use the latest flag; do not rely on a stale capture
            if (target == SheetValue.Hidden && !isSwipeRef.value) return@rememberModalBottomSheetState false
            true
        },
    )

    LaunchedEffect(phase) {
        if (programmaticDismiss && sheetState.currentValue != SheetValue.Hidden) {
            sheetState.hide()
            onDismissCompleteRef.value()
        }
    }

    ModalBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        onDismissRequest = { if (isSwipeRef.value) onDismiss() }, // latest flag
        sheetState = sheetState,
        scrimColor = AppTokens.colors.dialog.scrim,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = isSwipeDismissEnabled, // recomposes with new flag
        ),
        containerColor = AppTokens.colors.background.dialog,
        dragHandle = null,
        shape = RoundedCornerShape(
            topStart = AppTokens.dp.bottomSheet.radius,
            topEnd = AppTokens.dp.bottomSheet.radius
        ),
    ) {
        BottomSheetToolbar(
            modifier = Modifier.fillMaxWidth(),
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