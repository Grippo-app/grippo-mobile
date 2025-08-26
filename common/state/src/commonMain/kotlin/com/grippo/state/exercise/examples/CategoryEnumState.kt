package com.grippo.state.exercise.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.category_compound
import com.grippo.design.resources.provider.category_isolation
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class CategoryEnumState {
    COMPOUND,
    ISOLATION;

    @Composable
    public fun title(): String {
        val r = when (this) {
            COMPOUND -> Res.string.category_compound
            ISOLATION -> Res.string.category_isolation
        }
        return AppTokens.strings.res(r)
    }
}