package com.grippo.height.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.design.components.sheet.Sheet
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun HeightPickerScreen(
    state: HeightPickerState,
    loaders: ImmutableSet<HeightPickerLoader>,
    contract: HeightPickerContract
) {
    Sheet(
        onDismiss = contract::dismiss,
        content = { hide ->
            Box(
                modifier = Modifier.size(300.dp).background(Color.Red)
                    .clickable(onClick = hide)
            )
        }
    )
}