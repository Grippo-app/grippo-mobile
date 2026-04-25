package com.grippo.goal.setup.suggestion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_direction_description
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_direction_title
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_evolves_description
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_evolves_title
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_tailored_description
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_tailored_title
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.design.resources.provider.icons.Pro
import com.grippo.design.resources.provider.icons.Repeat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class GoalSetupSuggestionState(
    val benefits: ImmutableList<BenefitCode> = DEFAULT_BENEFITS,
) {

    @Immutable
    public enum class BenefitCode {
        Tailored,
        Direction,
        Evolves;

        @Composable
        public fun icon(): ImageVector = when (this) {
            Tailored -> AppTokens.icons.Pro
            Direction -> AppTokens.icons.LineUp
            Evolves -> AppTokens.icons.Repeat
        }


        @Composable
        public fun text(): Pair<String, String> = when (this) {
            Tailored ->
                AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_title) to
                        AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_description)

            Direction ->
                AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_direction_title) to
                        AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_direction_description)

            Evolves ->
                AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_evolves_title) to
                        AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_evolves_description)
        }
    }

    public companion object {
        internal val DEFAULT_BENEFITS: ImmutableList<BenefitCode> = persistentListOf(
            BenefitCode.Tailored,
            BenefitCode.Direction,
            BenefitCode.Evolves,
        )
    }
}
