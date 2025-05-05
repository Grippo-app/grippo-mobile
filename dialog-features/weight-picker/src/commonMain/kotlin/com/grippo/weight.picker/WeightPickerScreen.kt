package com.grippo.weight.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun WeightPickerScreen(
    state: WeightPickerState,
    loaders: ImmutableSet<WeightPickerLoader>,
    contract: WeightPickerContract
) {
    Box(
        modifier = Modifier.size(300.dp).background(Color.Green)
            .clickable { contract.dismiss() }
    )
}