package com.grippo.state.exercise.examples

import androidx.compose.runtime.Immutable
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.mostly_used
import com.grippo.design.resources.provider.new_added
import com.grippo.design.resources.provider.recently_used
import com.grippo.state.formatters.UiText
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public enum class ExampleSortingEnumState {
    MostlyUsed,
    RecentlyUsed,
    NewAdded;

    public fun title(): UiText {
        val r = when (this) {
            MostlyUsed -> Res.string.mostly_used
            RecentlyUsed -> Res.string.recently_used
            NewAdded -> Res.string.new_added
        }
        return UiText.Res(r)
    }
}
