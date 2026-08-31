package com.grippo.core.state.examples

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
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

    public fun title(): UiText = TITLES.getValue(this)

    public companion object {
        private val TITLES: Map<WeightTypeEnumState, UiText> = entries.associateWith {
            UiText.Res(
                when (it) {
                    FREE -> Res.string.weight_type_free
                    FIXED -> Res.string.weight_type_fixed
                    BODY_WEIGHT -> Res.string.weight_type_body_weight
                }
            )
        }
    }
}