package com.grippo.core.state.metrics.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile_kind_easy
import com.grippo.design.resources.provider.training_profile_kind_endurance
import com.grippo.design.resources.provider.training_profile_kind_hypertrophy
import com.grippo.design.resources.provider.training_profile_kind_mixed
import com.grippo.design.resources.provider.training_profile_kind_powerbuilding
import com.grippo.design.resources.provider.training_profile_kind_strength
import com.grippo.design.resources.provider.training_profile_subtitle_easy
import com.grippo.design.resources.provider.training_profile_subtitle_endurance
import com.grippo.design.resources.provider.training_profile_subtitle_hypertrophy
import com.grippo.design.resources.provider.training_profile_subtitle_mixed
import com.grippo.design.resources.provider.training_profile_subtitle_powerbuilding
import com.grippo.design.resources.provider.training_profile_subtitle_strength
import com.grippo.design.resources.provider.training_profile_tip_easy
import com.grippo.design.resources.provider.training_profile_tip_endurance_high
import com.grippo.design.resources.provider.training_profile_tip_endurance_low
import com.grippo.design.resources.provider.training_profile_tip_endurance_medium
import com.grippo.design.resources.provider.training_profile_tip_hypertrophy_high
import com.grippo.design.resources.provider.training_profile_tip_hypertrophy_low
import com.grippo.design.resources.provider.training_profile_tip_hypertrophy_medium
import com.grippo.design.resources.provider.training_profile_tip_mixed
import com.grippo.design.resources.provider.training_profile_tip_powerbuilding
import com.grippo.design.resources.provider.training_profile_tip_strength_high
import com.grippo.design.resources.provider.training_profile_tip_strength_low
import com.grippo.design.resources.provider.training_profile_tip_strength_medium
import com.grippo.design.resources.provider.training_profile_title_easy_session

@Immutable
public enum class TrainingProfileKindState {
    Strength,
    Hypertrophy,
    Endurance,
    Powerbuilding,
    Mixed,
    Easy;

    @Composable
    public fun label(): String {
        return when (this) {
            Strength -> AppTokens.strings.res(Res.string.training_profile_kind_strength)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_profile_kind_hypertrophy)
            Endurance -> AppTokens.strings.res(Res.string.training_profile_kind_endurance)
            Powerbuilding -> AppTokens.strings.res(Res.string.training_profile_kind_powerbuilding)
            Mixed -> AppTokens.strings.res(Res.string.training_profile_kind_mixed)
            Easy -> AppTokens.strings.res(Res.string.training_profile_kind_easy)
        }
    }

    @Composable
    public fun title(profile: TrainingLoadProfileState): String {
        return when (this) {
            Easy -> AppTokens.strings.res(Res.string.training_profile_title_easy_session)
            Powerbuilding -> label()
            Mixed -> label()
            Strength -> label()
            Hypertrophy -> label()
            Endurance -> label()
        }
    }

    @Composable
    public fun subtitle(profile: TrainingLoadProfileState): String {
        return when (this) {
            Easy -> AppTokens.strings.res(Res.string.training_profile_subtitle_easy)
            Powerbuilding -> AppTokens.strings.res(Res.string.training_profile_subtitle_powerbuilding)
            Mixed -> AppTokens.strings.res(Res.string.training_profile_subtitle_mixed)
            Strength -> AppTokens.strings.res(Res.string.training_profile_subtitle_strength)
            Hypertrophy -> AppTokens.strings.res(Res.string.training_profile_subtitle_hypertrophy)
            Endurance -> AppTokens.strings.res(Res.string.training_profile_subtitle_endurance)
        }
    }

    @Composable
    public fun tip(profile: TrainingLoadProfileState): String? {
        val c = profile.confidence.coerceIn(0, 100)
        return when (this) {
            Easy -> AppTokens.strings.res(Res.string.training_profile_tip_easy)
            Powerbuilding -> AppTokens.strings.res(Res.string.training_profile_tip_powerbuilding)
            Mixed -> AppTokens.strings.res(Res.string.training_profile_tip_mixed)
            Strength -> when {
                c >= 70 -> AppTokens.strings.res(Res.string.training_profile_tip_strength_high)
                c >= 35 -> AppTokens.strings.res(Res.string.training_profile_tip_strength_medium)
                else -> AppTokens.strings.res(Res.string.training_profile_tip_strength_low)
            }

            Hypertrophy -> when {
                c >= 70 -> AppTokens.strings.res(Res.string.training_profile_tip_hypertrophy_high)
                c >= 35 -> AppTokens.strings.res(Res.string.training_profile_tip_hypertrophy_medium)
                else -> AppTokens.strings.res(Res.string.training_profile_tip_hypertrophy_low)
            }

            Endurance -> when {
                c >= 70 -> AppTokens.strings.res(Res.string.training_profile_tip_endurance_high)
                c >= 35 -> AppTokens.strings.res(Res.string.training_profile_tip_endurance_medium)
                else -> AppTokens.strings.res(Res.string.training_profile_tip_endurance_low)
            }
        }
    }
}
