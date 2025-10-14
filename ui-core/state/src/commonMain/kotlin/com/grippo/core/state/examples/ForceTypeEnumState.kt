package com.grippo.core.state.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
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

    public fun title(): UiText {
        val r = when (this) {
            PULL -> Res.string.force_type_pull
            PUSH -> Res.string.force_type_push
            HINGE -> Res.string.force_type_hinge
        }

        return UiText.Res(r)
    }

    @Composable
    public fun color(): Color {
        return when (this) {
            PULL -> AppTokens.colors.example.forceType.pull
            PUSH -> AppTokens.colors.example.forceType.push
            HINGE -> AppTokens.colors.example.forceType.hinge
        }
    }
}