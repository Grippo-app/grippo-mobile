package com.grippo.training.completed.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.core.AppTokens

@Composable
internal fun ProgressDivider(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))
        ContentDivider()
        Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.content))
    }
}
