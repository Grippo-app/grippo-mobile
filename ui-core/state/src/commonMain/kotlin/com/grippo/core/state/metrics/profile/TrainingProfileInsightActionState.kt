package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile_insight_action_add_compound_anchor
import com.grippo.design.resources.provider.training_profile_insight_action_add_one_hard_set
import com.grippo.design.resources.provider.training_profile_insight_action_add_pulling_work
import com.grippo.design.resources.provider.training_profile_insight_action_add_pushing_work
import com.grippo.design.resources.provider.training_profile_insight_action_diversify_exercises
import com.grippo.design.resources.provider.training_profile_insight_action_pick_clearer_style
import com.grippo.design.resources.provider.training_profile_insight_action_spread_muscle_focus

@Immutable
public enum class TrainingProfileInsightActionState {
    AddCompoundAnchor,
    DiversifyExercises,
    AddPullingWork,
    AddPushingWork,
    SpreadMuscleFocus,
    AddOneHardSet,
    PickAClearerStyle;

    @Composable
    public fun text(): String = when (this) {
        AddCompoundAnchor ->
            AppTokens.strings.res(Res.string.training_profile_insight_action_add_compound_anchor)

        DiversifyExercises ->
            AppTokens.strings.res(Res.string.training_profile_insight_action_diversify_exercises)

        AddPullingWork ->
            AppTokens.strings.res(Res.string.training_profile_insight_action_add_pulling_work)

        AddPushingWork ->
            AppTokens.strings.res(Res.string.training_profile_insight_action_add_pushing_work)

        SpreadMuscleFocus ->
            AppTokens.strings.res(Res.string.training_profile_insight_action_spread_muscle_focus)

        AddOneHardSet ->
            AppTokens.strings.res(Res.string.training_profile_insight_action_add_one_hard_set)

        PickAClearerStyle ->
            AppTokens.strings.res(Res.string.training_profile_insight_action_pick_clearer_style)
    }
}
