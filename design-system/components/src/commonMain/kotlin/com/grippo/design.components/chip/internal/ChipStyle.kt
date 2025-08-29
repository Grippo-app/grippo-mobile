package com.grippo.design.components.chip.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.core.AppTokens

@Stable
internal data class ChipStyle(
    val radius: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val trailingSize: Dp,
    val spaceBetween: Dp,
    val labelTextStyle: TextStyle,
    val valueTextStyle: TextStyle
)

@Composable
internal fun resolveChipStyle(size: ChipSize): ChipStyle {
    val dp = AppTokens.dp.chip
    return when (size) {
        ChipSize.Small -> ChipStyle(
            radius = dp.small.radius,
            horizontalPadding = dp.small.horizontalPadding,
            verticalPadding = dp.small.verticalPadding,
            trailingSize = dp.small.trailingSize,
            spaceBetween = dp.small.spaceBetween,
            labelTextStyle = AppTokens.typography.b11Semi(),
            valueTextStyle = AppTokens.typography.b11Bold(),
        )

        ChipSize.Medium -> ChipStyle(
            radius = dp.medium.radius,
            horizontalPadding = dp.medium.horizontalPadding,
            verticalPadding = dp.medium.verticalPadding,
            trailingSize = dp.medium.trailingSize,
            spaceBetween = dp.medium.spaceBetween,
            labelTextStyle = AppTokens.typography.b14Semi(),
            valueTextStyle = AppTokens.typography.b14Bold(),
        )
    }
}