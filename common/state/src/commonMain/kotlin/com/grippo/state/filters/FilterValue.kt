package com.grippo.state.filters

import androidx.compose.runtime.Immutable
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.filter_category
import com.grippo.design.resources.provider.filter_force_type
import com.grippo.design.resources.provider.filter_weight_type
import com.grippo.state.exercise.examples.CategoryEnumState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.formatters.UiText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public sealed interface FilterValue {

    public fun title(): UiText

    @Serializable
    @Immutable
    public data class WeightType(
        val value: WeightTypeEnumState,
        val available: ImmutableList<WeightTypeEnumState> = WeightTypeEnumState.entries.toPersistentList()
    ) : FilterValue {

        public override fun title(): UiText {
            return UiText.Res(Res.string.filter_weight_type)
        }
    }

    @Serializable
    @Immutable
    public data class ForceType(
        val value: ForceTypeEnumState,
        val available: ImmutableList<ForceTypeEnumState> = ForceTypeEnumState.entries.toPersistentList()
    ) : FilterValue {

        public override fun title(): UiText {
            return UiText.Res(Res.string.filter_force_type)
        }
    }

    @Serializable
    @Immutable
    public data class Category(
        val value: CategoryEnumState,
        val available: ImmutableList<CategoryEnumState> = CategoryEnumState.entries.toPersistentList()
    ) : FilterValue {

        public override fun title(): UiText {
            return UiText.Res(Res.string.filter_category)
        }
    }
}