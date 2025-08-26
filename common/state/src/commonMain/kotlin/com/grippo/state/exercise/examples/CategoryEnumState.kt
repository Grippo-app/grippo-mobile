package com.grippo.state.exercise.examples

import androidx.compose.runtime.Immutable
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.category_compound
import com.grippo.design.resources.provider.category_isolation
import com.grippo.state.formatters.UiText
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class CategoryEnumState {
    COMPOUND,
    ISOLATION;

    public fun title(): UiText {
        val r = when (this) {
            COMPOUND -> Res.string.category_compound
            ISOLATION -> Res.string.category_isolation
        }
        return UiText.Res(r)
    }
}