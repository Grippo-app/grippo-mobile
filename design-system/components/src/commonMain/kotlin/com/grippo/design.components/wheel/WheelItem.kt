package com.grippo.design.components.wheel

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens

@Composable
public fun WheelItem(
    modifier: Modifier = Modifier,
    text: String,
    isValid: Boolean
) {
    Text(
        modifier = modifier,
        text = text,
        style = AppTokens.typography.h5(),
        color = if (isValid) AppTokens.colors.text.primary else AppTokens.colors.semantic.error
    )
}