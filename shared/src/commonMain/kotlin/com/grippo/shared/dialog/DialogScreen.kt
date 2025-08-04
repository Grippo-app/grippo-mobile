package com.grippo.shared.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.bottom.sheet.BottomSheetBackButton
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
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.primary)) {
    val slotState = component.childSlot.subscribeAsState()
    val child = slotState.value.child ?: return@BaseComposeScreen

    val contentComponent = (child.instance.component as? DialogContentComponent)
        ?: return@BaseComposeScreen

    DialogSheet(
        config = child.configuration,
        stackSize = state.stack.size,
        process = state.process,
        component = contentComponent,
        onBack = { contract.onDismiss(null) },
        onDismiss = { contract.onRelease(child.configuration) },
        onDismissComplete = { contract.onRelease(child.configuration) }
    )
}

@Composable
private fun DialogSheet(
    config: DialogConfig,
    stackSize: Int,
    process: Process,
    component: DialogContentComponent,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onDismissComplete: () -> Unit
) {
    val modalSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true },
    )

    // Trigger hide + release only once when process is DISMISS
    LaunchedEffect(process) {
        if (process == Process.DISMISS) {
            modalSheetState.hide()
            onDismissComplete()
        }
    }

    ModalBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        onDismissRequest = onDismiss,
        sheetState = modalSheetState,
        scrimColor = AppTokens.colors.dialog.scrim,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
        containerColor = AppTokens.colors.dialog.background,
        dragHandle = null,
        shape = RoundedCornerShape(
            topStart = AppTokens.dp.bottomSheet.radius,
            topEnd = AppTokens.dp.bottomSheet.radius
        ),
    ) {
        component.Render()

        AnimatedContent(
            targetState = stackSize > 1,
            label = "BackButtonAnimation",
            transitionSpec = {
                fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
            },
            content = { show ->
                if (show) BottomSheetBackButton(onClick = onBack)
            }
        )
    }
}