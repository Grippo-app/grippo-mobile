package com.grippo.design.components.wheel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun WheelItemRow(
    modifier: Modifier = Modifier,
    text: String,
    subText: String? = null,
    isValid: Boolean,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
) {
    Row(
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement
    ) {
        Text(
            modifier = Modifier,
            text = text,
            style = AppTokens.typography.h5(),
            color = if (isValid) AppTokens.colors.text.primary else AppTokens.colors.semantic.error
        )

        subText?.let {
            Spacer(Modifier.width(AppTokens.dp.contentPadding.text))
            Text(
                modifier = Modifier,
                text = subText,
                style = AppTokens.typography.h5(),
                color = if (isValid) AppTokens.colors.text.tertiary else AppTokens.colors.semantic.error
            )
        }
    }
}

@AppPreview
@Composable
private fun WheelItemRowValidPreview() {
    PreviewContainer {
        WheelItemRow(
            text = "100",
            subText = "cm",
            isValid = true
        )
    }
}

@AppPreview
@Composable
private fun WheelItemRowInvalidPreview() {
    PreviewContainer {
        WheelItemRow(
            text = "0",
            subText = "kg",
            isValid = false
        )
    }
}