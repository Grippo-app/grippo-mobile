package com.grippo.core.state.examples

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.force_type_hinge
import com.grippo.design.resources.provider.force_type_pull
import com.grippo.design.resources.provider.force_type_push
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class ForceTypeEnumState {
    PULL,
    PUSH,
    HINGE;

    public fun title(): UiText = TITLES.getValue(this)

    public companion object {
        private val TITLES: Map<ForceTypeEnumState, UiText> = entries.associateWith {
            UiText.Res(
                when (it) {
                    PULL -> Res.string.force_type_pull
                    PUSH -> Res.string.force_type_push
                    HINGE -> Res.string.force_type_hinge
                }
            )
        }
    }
}