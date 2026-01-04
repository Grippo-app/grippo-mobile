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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.toolbar.BottomSheetToolbar
import com.grippo.design.core.AppTokens
import com.grippo.dialog.api.DialogConfig
import com.grippo.shared.dialog.content.DialogContentComponent
import kotlinx.collections.immutable.ImmutableSet
import kotlin.coroutines.cancellation.CancellationException

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
    val programmaticDismiss = phase == SheetPhase.Dismissing

    val programmaticDismissRef = rememberUpdatedState(programmaticDismiss)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            // Use the latest flag; do not rely on a stale capture
            if (target == SheetValue.Hidden && !isSwipeRef.value) return@rememberModalBottomSheetState false
            true
        },
    )

    var dismissNotified by remember { mutableStateOf(false) }

    LaunchedEffect(phase) {
        if (programmaticDismiss) {
            dismissNotified = false
            if (sheetState.currentValue != SheetValue.Hidden) {
                sheetState.hide() // suspends until the hide animation finishes
            }
            if (!dismissNotified) {
                onDismissCompleteRef.value()
                dismissNotified = true
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { sheetState.currentValue }.collect { value ->
            if (value == SheetValue.Hidden && programmaticDismissRef.value && !dismissNotified) {
                onDismissCompleteRef.value()
                dismissNotified = true
            }
        }
    }

    // If VM says PRESENT but the sheet is Hidden (e.g., new content after a prior hide),
    // explicitly open it. This does not affect in-sheet Push/Pop when already visible.
    LaunchedEffect(phase, config) {
        if (phase == SheetPhase.Present) {
            withFrameNanos { /* next frame */ }

            val animInProgress = sheetState.currentValue != sheetState.targetValue
            val isHiddenNow = sheetState.currentValue == SheetValue.Hidden
            val targetIsHidden = sheetState.targetValue == SheetValue.Hidden

            if (!animInProgress && isHiddenNow && targetIsHidden) {
                try {
                    sheetState.show()
                } catch (_: CancellationException) {
                }
            }
        }
    }

    ModalBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        onDismissRequest = { if (isSwipeRef.value) onDismiss() }, // latest flag
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets() },
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