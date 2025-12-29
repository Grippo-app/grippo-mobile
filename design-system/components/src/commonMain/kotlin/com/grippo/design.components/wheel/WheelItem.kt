package com.grippo.design.components.wheel

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

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

@AppPreview
@Composable
private fun WheelItemValidPreview() {
    PreviewContainer {
        WheelItem(
            text = "100 kg",
            isValid = true
        )
    }
}

@AppPreview
@Composable
private fun WheelItemInvalidPreview() {
    PreviewContainer {
        WheelItem(
            text = "0 kg",
            isValid = false
        )
    }
}