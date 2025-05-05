package com.grippo.design.components.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import kotlinx.coroutines.launch

@Composable
public fun Sheet(
    modifier: Modifier = Modifier,
    cancelable: Boolean = true,
    onDismiss: () -> Unit = {},
    content: @Composable ColumnScope.(hideBottomSheet: () -> Unit) -> Unit,
) {
    val modalBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { cancelable },
    )

    val coroutineScope = rememberCoroutineScope()
    val dismissProvider: () -> Unit = remember(onDismiss) {
        {
            coroutineScope.launch {
                modalBottomSheetState.hide()
                onDismiss.invoke()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = dismissProvider,
        sheetState = modalBottomSheetState,
        scrimColor = AppTokens.colors.bottomSheet.scrim,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = cancelable),
        containerColor = AppTokens.colors.bottomSheet.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = AppTokens.colors.bottomSheet.handle
            )
        },
        content = {
            Column(modifier = modifier.navigationBarsPadding()) {
                content.invoke(this, dismissProvider)
            }
        },
    )
}