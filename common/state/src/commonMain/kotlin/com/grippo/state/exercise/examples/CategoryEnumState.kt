package com.grippo.state.exercise.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.design.core.AppTokens
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

    @Composable
    public fun color(): Color {
        return when (this) {
            COMPOUND -> AppTokens.colors.example.category.compound
            ISOLATION -> AppTokens.colors.example.category.isolation
        }
    }
}