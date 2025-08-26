package com.grippo.state.exercise.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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

    @Composable
    public fun title(): String {
        val r = when (this) {
            PULL -> Res.string.force_type_pull
            PUSH -> Res.string.force_type_push
            HINGE -> Res.string.force_type_hinge
        }

        return AppTokens.strings.res(r)
    }
}