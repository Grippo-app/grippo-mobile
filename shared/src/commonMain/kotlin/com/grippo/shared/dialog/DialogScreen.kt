package com.grippo.shared.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.back
import com.grippo.design.resources.icons.NavArrowLeft
import com.grippo.dialog.api.DialogConfig
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun DialogScreen(
    slot: Value<ChildSlot<DialogConfig, DialogComponent.Child>>,
    state: DialogState,
    loaders: ImmutableSet<DialogLoader>,
    contract: DialogContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.primary)) {
    val slotState = slot.subscribeAsState()

    val child = slotState.value.child
    val component = child?.instance?.component

    if (component != null) {
        val config = child.configuration

        val backProvider = remember(state.stack.size) {
            if (state.stack.size > 1) {
                { contract.pop() }
            } else null
        }

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
            modifier = Modifier.statusBarsPadding(),
            onDismissRequest = { contract.release(config) },
            sheetState = modalBottomSheetState,
            scrimColor = AppTokens.colors.dialog.scrim,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
            containerColor = AppTokens.colors.dialog.background,
            dragHandle = null,
            shape = RoundedCornerShape(
                topStart = AppTokens.dp.bottomSheet.radius,
                topEnd = AppTokens.dp.bottomSheet.radius
            ),
            content = {
                AnimatedContent(targetState = component to backProvider) {
                    Column {
                        Column(
                            modifier = Modifier.weight(1f, false),
                            content = { component.Render() }
                        )

                        backProvider?.let { back ->

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = AppTokens.colors.divider.secondary
                            )

                            Spacer(Modifier.height(AppTokens.dp.screen.verticalPadding))

                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                                text = AppTokens.strings.res(Res.string.back),
                                startIcon = AppTokens.icons.NavArrowLeft,
                                style = ButtonStyle.Transparent,
                                onClick = back
                            )

                            Spacer(Modifier.height(AppTokens.dp.screen.verticalPadding))
                        }
                    }
                }
            },
        )
    }
}