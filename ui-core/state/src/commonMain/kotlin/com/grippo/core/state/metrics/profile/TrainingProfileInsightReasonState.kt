package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile_insight_balanced_exercise_spread_detail
import com.grippo.design.resources.provider.training_profile_insight_balanced_exercise_spread_headline
import com.grippo.design.resources.provider.training_profile_insight_clear_dominant_detail
import com.grippo.design.resources.provider.training_profile_insight_clear_dominant_headline
import com.grippo.design.resources.provider.training_profile_insight_compound_foundation_detail
import com.grippo.design.resources.provider.training_profile_insight_compound_foundation_headline
import com.grippo.design.resources.provider.training_profile_insight_easy_session_detail
import com.grippo.design.resources.provider.training_profile_insight_easy_session_headline
import com.grippo.design.resources.provider.training_profile_insight_high_concentration_detail
import com.grippo.design.resources.provider.training_profile_insight_high_concentration_headline
import com.grippo.design.resources.provider.training_profile_insight_isolation_heavy_detail
import com.grippo.design.resources.provider.training_profile_insight_isolation_heavy_headline
import com.grippo.design.resources.provider.training_profile_insight_low_confidence_detail
import com.grippo.design.resources.provider.training_profile_insight_low_confidence_headline
import com.grippo.design.resources.provider.training_profile_insight_mixed_session_detail
import com.grippo.design.resources.provider.training_profile_insight_mixed_session_headline
import com.grippo.design.resources.provider.training_profile_insight_narrow_muscle_focus_detail
import com.grippo.design.resources.provider.training_profile_insight_narrow_muscle_focus_headline
import com.grippo.design.resources.provider.training_profile_insight_powerbuilding_detail
import com.grippo.design.resources.provider.training_profile_insight_powerbuilding_headline
import com.grippo.design.resources.provider.training_profile_insight_push_pull_imbalance_detail
import com.grippo.design.resources.provider.training_profile_insight_push_pull_imbalance_headline

@Immutable
public enum class TrainingProfileInsightReasonState {
    EasySession,
    MixedSession,
    PowerbuildingPattern,
    ClearDominant,
    LowConfidence,
    CompoundFoundation,
    IsolationHeavy,
    HighExerciseConcentration,
    BalancedExerciseSpread,
    PushPullImbalance,
    NarrowMuscleFocus;

    @Composable
    public fun headline(): String = when (this) {
        EasySession ->
            AppTokens.strings.res(Res.string.training_profile_insight_easy_session_headline)

        MixedSession ->
            AppTokens.strings.res(Res.string.training_profile_insight_mixed_session_headline)

        PowerbuildingPattern ->
            AppTokens.strings.res(Res.string.training_profile_insight_powerbuilding_headline)

        ClearDominant ->
            AppTokens.strings.res(Res.string.training_profile_insight_clear_dominant_headline)

        LowConfidence ->
            AppTokens.strings.res(Res.string.training_profile_insight_low_confidence_headline)

        CompoundFoundation ->
            AppTokens.strings.res(Res.string.training_profile_insight_compound_foundation_headline)

        IsolationHeavy ->
            AppTokens.strings.res(Res.string.training_profile_insight_isolation_heavy_headline)

        HighExerciseConcentration ->
            AppTokens.strings.res(Res.string.training_profile_insight_high_concentration_headline)

        BalancedExerciseSpread ->
            AppTokens.strings.res(Res.string.training_profile_insight_balanced_exercise_spread_headline)

        PushPullImbalance ->
            AppTokens.strings.res(Res.string.training_profile_insight_push_pull_imbalance_headline)

        NarrowMuscleFocus ->
            AppTokens.strings.res(Res.string.training_profile_insight_narrow_muscle_focus_headline)
    }

    @Composable
    public fun detail(): String = when (this) {
        EasySession ->
            AppTokens.strings.res(Res.string.training_profile_insight_easy_session_detail)

        MixedSession ->
            AppTokens.strings.res(Res.string.training_profile_insight_mixed_session_detail)

        PowerbuildingPattern ->
            AppTokens.strings.res(Res.string.training_profile_insight_powerbuilding_detail)

        ClearDominant ->
            AppTokens.strings.res(Res.string.training_profile_insight_clear_dominant_detail)

        LowConfidence ->
            AppTokens.strings.res(Res.string.training_profile_insight_low_confidence_detail)

        CompoundFoundation ->
            AppTokens.strings.res(Res.string.training_profile_insight_compound_foundation_detail)

        IsolationHeavy ->
            AppTokens.strings.res(Res.string.training_profile_insight_isolation_heavy_detail)

        HighExerciseConcentration ->
            AppTokens.strings.res(Res.string.training_profile_insight_high_concentration_detail)

        BalancedExerciseSpread ->
            AppTokens.strings.res(Res.string.training_profile_insight_balanced_exercise_spread_detail)

        PushPullImbalance ->
            AppTokens.strings.res(Res.string.training_profile_insight_push_pull_imbalance_detail)

        NarrowMuscleFocus ->
            AppTokens.strings.res(Res.string.training_profile_insight_narrow_muscle_focus_detail)
    }
}
