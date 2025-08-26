package com.grippo.state.exercise.examples

import androidx.compose.runtime.Immutable
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.weight_type_body_weight
import com.grippo.design.resources.provider.weight_type_fixed
import com.grippo.design.resources.provider.weight_type_free
import com.grippo.state.formatters.UiText
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
}