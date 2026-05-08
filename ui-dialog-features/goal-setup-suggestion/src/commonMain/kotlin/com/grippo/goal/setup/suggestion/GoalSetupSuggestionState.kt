package com.grippo.goal.setup.suggestion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_tailored_description
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_tailored_title
import com.grippo.design.resources.provider.icons.Medal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class GoalSetupSuggestionState(
    val benefits: ImmutableList<BenefitCode> = DEFAULT_BENEFITS,
) {

    @Immutable
    public enum class BenefitCode {
        Tailored;

        @Composable
        public fun icon(): ImageVector = when (this) {
            Tailored -> AppTokens.icons.Medal
        }

        @Composable
        public fun text(): Pair<String, String> = when (this) {
            Tailored ->
                AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_title) to
                        AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_description)
        }
    }

    public companion object {
        internal val DEFAULT_BENEFITS: ImmutableList<BenefitCode> = persistentListOf(
            BenefitCode.Tailored,
        )
    }
}
