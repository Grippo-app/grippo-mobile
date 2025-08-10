package com.grippo.shared.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.toolbar.BottomSheetToolbar
import com.grippo.design.components.toolbar.BottomSheetToolbarActionButton
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.back
import com.grippo.design.resources.provider.close
import com.grippo.design.resources.provider.icons.NavArrowLeft
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
        stackSize = state.stack.size,
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
    stackSize: Int,
    phase: SheetPhase,
    component: DialogContentComponent,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onDismiss: () -> Unit,
    onDismissComplete: () -> Unit
) {
    val onDismissCompleteRef = rememberUpdatedState(onDismissComplete)

    val showBackButton = stackSize > 1
    val isSwipeDismissEnabled = config.dismissBySwipe
    val programmaticDismiss = phase == SheetPhase.DISMISSING
    val showHeader = showBackButton || isSwipeDismissEnabled.not()

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && !isSwipeDismissEnabled) return@rememberModalBottomSheetState false
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
        onDismissRequest = { if (isSwipeDismissEnabled) onDismiss() },
        sheetState = sheetState,
        scrimColor = AppTokens.colors.dialog.scrim,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = isSwipeDismissEnabled,
        ),
        containerColor = AppTokens.colors.background.dialog,
        dragHandle = null,
        shape = RoundedCornerShape(
            topStart = AppTokens.dp.bottomSheet.radius,
            topEnd = AppTokens.dp.bottomSheet.radius
        ),
    ) {
        AnimatedVisibility(
            visible = showHeader,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            BottomSheetToolbar(
                modifier = Modifier.fillMaxWidth(),
                start = if (showBackButton) BottomSheetToolbarActionButton(
                    text = AppTokens.strings.res(Res.string.back),
                    startIcon = AppTokens.icons.NavArrowLeft,
                    style = ButtonStyle.Transparent,
                    onClick = onBack
                ) else null,
                end = if (isSwipeDismissEnabled.not()) BottomSheetToolbarActionButton(
                    text = AppTokens.strings.res(Res.string.close),
                    startIcon = null,
                    style = ButtonStyle.Transparent,
                    onClick = onClose
                ) else null,
            )
        }

        Column(
            modifier = Modifier.weight(weight = 1f, fill = false),
            content = { component.Render() }
        )
    }
}