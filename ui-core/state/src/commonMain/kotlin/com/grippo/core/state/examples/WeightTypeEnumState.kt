package com.grippo.core.state.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.weight_type_body_weight
import com.grippo.design.resources.provider.weight_type_fixed
import com.grippo.design.resources.provider.weight_type_free
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class WeightTypeEnumState {
    FREE,
    FIXED,
    BODY_WEIGHT;

    public fun title(): UiText {
        val r = when (this) {
            FREE -> Res.string.weight_type_free
            FIXED -> Res.string.weight_type_fixed
            BODY_WEIGHT -> Res.string.weight_type_body_weight
        }
        return UiText.Res(r)
    }

    @Composable
    public fun color(): Color {
        return when (this) {
            FREE -> AppTokens.colors.example.weightType.free
            FIXED -> AppTokens.colors.example.weightType.fixed
            BODY_WEIGHT -> AppTokens.colors.example.weightType.bodyWeight
        }
    }
}