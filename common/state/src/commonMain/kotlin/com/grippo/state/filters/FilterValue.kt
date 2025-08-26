package com.grippo.state.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.filter_category
import com.grippo.design.resources.provider.filter_force_type
import com.grippo.design.resources.provider.filter_weight_type
import com.grippo.state.exercise.examples.CategoryEnumState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.exercise.examples.WeightTypeEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public sealed interface FilterValue {

    @Composable
    public fun title(): String

    @Serializable
    @Immutable
    public data class WeightType(
        val value: WeightTypeEnumState,
        val available: ImmutableList<WeightTypeEnumState> = WeightTypeEnumState.entries.toPersistentList()
    ) : FilterValue {

        @Composable
        public override fun title(): String {
            return AppTokens.strings.res(Res.string.filter_weight_type)
        }
    }

    @Serializable
    @Immutable
    public data class ForceType(
        val value: ForceTypeEnumState,
        val available: ImmutableList<ForceTypeEnumState> = ForceTypeEnumState.entries.toPersistentList()
    ) : FilterValue {

        @Composable
        public override fun title(): String {
            return AppTokens.strings.res(Res.string.filter_force_type)
        }
    }

    @Serializable
    @Immutable
    public data class Category(
        val value: CategoryEnumState,
        val available: ImmutableList<CategoryEnumState> = CategoryEnumState.entries.toPersistentList()
    ) : FilterValue {

        @Composable
        public override fun title(): String {
            return AppTokens.strings.res(Res.string.filter_category)
        }
    }
}