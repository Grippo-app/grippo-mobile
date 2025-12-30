package com.grippo.design.components.muscle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.grippo.core.state.muscles.metrics.MuscleLoadEntry
import com.grippo.design.core.AppTokens

@Composable
public fun MuscleLoading(
    entries: List<MuscleLoadEntry>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        entries.forEachIndexed { index, entry ->
            key(index) {
                MuscleLoadingEntry(
                    entry = entry,
                    dominant = index == 0
                )
            }
        }
    }
}
