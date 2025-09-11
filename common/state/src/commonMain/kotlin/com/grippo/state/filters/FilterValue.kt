package com.grippo.state.filters

import androidx.compose.runtime.Immutable
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.filter_category
import com.grippo.design.resources.provider.filter_experience
import com.grippo.design.resources.provider.filter_force_type
import com.grippo.design.resources.provider.filter_weight_type
import com.grippo.state.exercise.examples.CategoryEnumState
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.formatters.UiText
import com.grippo.state.profile.ExperienceEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public sealed interface FilterValue {
    public val id: String

    public fun isSelected(): Boolean

    public fun title(): UiText

    @Serializable
    @Immutable
    public data class WeightType(
        override val id: String = "filter_value_weight_type",
        val value: WeightTypeEnumState?,
        val available: ImmutableList<WeightTypeEnumState> = WeightTypeEnumState.entries.toPersistentList()
    ) : FilterValue {

        override fun isSelected(): Boolean {
            return value != null
        }

        public override fun title(): UiText {
            return UiText.Res(Res.string.filter_weight_type)
        }
    }

    @Serializable
    @Immutable
    public data class ForceType(
        override val id: String = "filter_value_force_type",
        val value: ForceTypeEnumState?,
        val available: ImmutableList<ForceTypeEnumState> = ForceTypeEnumState.entries.toPersistentList()
    ) : FilterValue {

        override fun isSelected(): Boolean {
            return value != null
        }

        public override fun title(): UiText {
            return UiText.Res(Res.string.filter_force_type)
        }
    }

    @Serializable
    @Immutable
    public data class Experience(
        override val id: String = "filter_value_experience",
        val value: ExperienceEnumState?,
        val available: ImmutableList<ExperienceEnumState> = ExperienceEnumState.entries.toPersistentList()
    ) : FilterValue {

        override fun isSelected(): Boolean {
            return value != null
        }

        public override fun title(): UiText {
            return UiText.Res(Res.string.filter_experience)
        }
    }

    @Serializable
    @Immutable
    public data class Category(
        override val id: String = "filter_value_category",
        val value: CategoryEnumState?,
        val available: ImmutableList<CategoryEnumState> = CategoryEnumState.entries.toPersistentList()
    ) : FilterValue {

        override fun isSelected(): Boolean {
            return value != null
        }

        public override fun title(): UiText {
            return UiText.Res(Res.string.filter_category)
        }
    }
}

public fun stubFilters(): ImmutableList<FilterValue> = ExerciseExampleState.filters