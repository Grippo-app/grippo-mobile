package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_dimension_endurance
import com.grippo.design.resources.provider.training_dimension_hypertrophy
import com.grippo.design.resources.provider.training_dimension_strength
import com.grippo.design.resources.provider.training_profile_axis_endurance_id
import com.grippo.design.resources.provider.training_profile_axis_hypertrophy_id
import com.grippo.design.resources.provider.training_profile_axis_strength_id

@Immutable
public enum class TrainingDimensionKindState {
    Strength,
    Hypertrophy,
    Endurance;

    @Composable
    public fun axisId(): String {
        return when (this) {
            Strength -> AppTokens.strings.res(Res.string.training_profile_axis_strength_id)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_profile_axis_hypertrophy_id)
            Endurance -> AppTokens.strings.res(Res.string.training_profile_axis_endurance_id)
        }
    }

    @Composable
    public fun label(): String {
        return when (this) {
            Strength -> AppTokens.strings.res(Res.string.training_dimension_strength)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_dimension_hypertrophy)
            Endurance -> AppTokens.strings.res(Res.string.training_dimension_endurance)
        }
    }
}
