package com.grippo.core.state.examples

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.category_compound
import com.grippo.design.resources.provider.category_isolation
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class CategoryEnumState {
    COMPOUND,
    ISOLATION;

    public fun title(): UiText = TITLES.getValue(this)

    public companion object {
        private val TITLES: Map<CategoryEnumState, UiText> = entries.associateWith {
            UiText.Res(
                when (it) {
                    COMPOUND -> Res.string.category_compound
                    ISOLATION -> Res.string.category_isolation
                }
            )
        }
    }
}